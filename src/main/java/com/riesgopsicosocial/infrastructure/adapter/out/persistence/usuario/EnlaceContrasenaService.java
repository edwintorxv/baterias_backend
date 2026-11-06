package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import com.riesgopsicosocial.infrastructure.security.UsuarioActual;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Function;

/**
 * Enlaces para definir la contraseña. El administrador nunca ve ni escribe contraseñas: genera un enlace
 * que llega por correo al usuario. Al generarlo, en la misma transacción, la contraseña actual deja de
 * servir, se cierran todas las sesiones del usuario y se anulan sus enlaces anteriores sin usar.
 */
@Service
public class EnlaceContrasenaService {

    static final Duration VIGENCIA = Duration.ofHours(48);
    private static final int BYTES_CODIGO = 32;

    private final UsuarioJpaRepository usuarioRepository;
    private final EnlaceContrasenaJpaRepository enlaceRepository;
    private final ApplicationEventPublisher eventos;
    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public EnlaceContrasenaService(UsuarioJpaRepository usuarioRepository,
                                   EnlaceContrasenaJpaRepository enlaceRepository,
                                   ApplicationEventPublisher eventos, PasswordEncoder passwordEncoder) {
        this(usuarioRepository, enlaceRepository, eventos, passwordEncoder, Clock.systemDefaultZone());
    }

    EnlaceContrasenaService(UsuarioJpaRepository usuarioRepository, EnlaceContrasenaJpaRepository enlaceRepository,
                            ApplicationEventPublisher eventos, PasswordEncoder passwordEncoder, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.enlaceRepository = enlaceRepository;
        this.eventos = eventos;
        this.passwordEncoder = passwordEncoder;
        this.reloj = reloj;
    }

    /**
     * Genera el enlace y lo envía por correo (después del commit). Es invitación si el usuario nunca ha
     * tenido contraseña; si no, restablecer. 409 si está inactivo o es uno mismo (para eso está "Cambiar
     * mi contraseña", y así un administrador no se deja por fuera).
     *
     * @return el usuario, para el mensaje de la respuesta (el enlace no se devuelve: va solo por correo)
     */
    @Transactional
    public UsuarioEntity generar(Long idUsuario) {
        return generar(usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + idUsuario)));
    }

    /** Igual que {@link #generar(Long)}, con el usuario ya cargado (p. ej. recién creado, misma transacción). */
    @Transactional
    public UsuarioEntity generar(UsuarioEntity usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessException("El usuario está inactivo: actívelo antes de generar el enlace");
        }
        if (usuario.getId().equals(UsuarioActual.id())) {
            throw new BusinessException("No puede generarse un enlace a sí mismo: use \"Cambiar mi contraseña\"");
        }
        PropositoEnlace proposito = usuario.getContrasenaHash() != null || enlaceRepository.yaTuvoContrasena(usuario.getId())
                ? PropositoEnlace.RESTABLECER : PropositoEnlace.INVITACION;
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String quien = UsuarioActual.nombre();

        enlaceRepository.anularSinUsar(usuario.getId(), ahora, quien);
        usuario.setContrasenaHash(null);
        cerrarSesiones(usuario, ahora);
        usuario.setFechaModificado(ahora);
        usuario.setUsuarioModifica(quien);
        usuarioRepository.save(usuario);

        String codigo = nuevoCodigo();
        EnlaceContrasenaEntity enlace = new EnlaceContrasenaEntity();
        enlace.setFkUsuario(usuario.getId());
        enlace.setCodigoHash(hash(codigo));
        enlace.setProposito(proposito);
        enlace.setVence(ahora.plus(VIGENCIA));
        enlace.setUsuarioCrea(quien);
        enlaceRepository.save(enlace);

        eventos.publishEvent(new EnlaceContrasenaGenerado(usuario.getCorreo(), usuario.getNombre(), proposito,
                codigo, enlace.getVence()));
        return usuario;
    }

    /**
     * Estado del enlace para la página pública: nombre y correo solo si está vigente. Un código que no
     * existe o de un usuario inactivo responde INVALIDO, sin decir más.
     */
    @Transactional(readOnly = true)
    public ConsultaEnlace consultar(String codigo) {
        Optional<EnlaceContrasenaEntity> enlace = buscar(codigo, enlaceRepository::findByCodigoHash);
        Optional<UsuarioEntity> usuario = enlace.flatMap(e -> usuarioRepository.findById(e.getFkUsuario()));
        EstadoEnlace estado = estado(enlace, usuario, LocalDateTime.now(reloj));
        if (estado != EstadoEnlace.VIGENTE) {
            return new ConsultaEnlace(estado, null, null, null, null);
        }
        EnlaceContrasenaEntity e = enlace.get();
        return new ConsultaEnlace(estado, e.getProposito(), usuario.get().getNombre(), usuario.get().getCorreo(), e.getVence());
    }

    /**
     * Guarda la contraseña que el usuario escogió, marca el enlace como usado, cierra sus sesiones y le avisa
     * por correo (después del commit, sin la contraseña). El
     * enlace se lee con bloqueo: dos envíos a la vez del mismo enlace no pasan los dos. 409 si no está
     * vigente, con un mensaje que lo remite al administrador.
     *
     * @return el usuario, para que el front lleve su correo al inicio de sesión
     */
    @Transactional
    public UsuarioEntity definirContrasena(String codigo, String contrasena) {
        Optional<EnlaceContrasenaEntity> enlace = buscar(codigo, enlaceRepository::buscarParaUsar);
        Optional<UsuarioEntity> usuario = enlace.flatMap(e -> usuarioRepository.findById(e.getFkUsuario()));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        EstadoEnlace estado = estado(enlace, usuario, ahora);
        if (estado != EstadoEnlace.VIGENTE) {
            throw new BusinessException(estado.mensaje());
        }
        UsuarioEntity u = usuario.get();
        EnlaceContrasenaEntity e = enlace.get();
        boolean primeraVez = e.getProposito() == PropositoEnlace.INVITACION;
        e.setFechaUso(ahora);
        e.setFechaModificado(ahora);
        e.setUsuarioModifica(u.getCorreo());
        enlaceRepository.save(e);

        u.setContrasenaHash(passwordEncoder.encode(contrasena));
        cerrarSesiones(u, ahora);
        u.setFechaModificado(ahora);
        u.setUsuarioModifica(u.getCorreo());
        usuarioRepository.save(u);
        eventos.publishEvent(new ContrasenaCambiada(u.getCorreo(), u.getNombre(), ahora, primeraVez));
        return u;
    }

    private static Optional<EnlaceContrasenaEntity> buscar(String codigo,
                                                          Function<String, Optional<EnlaceContrasenaEntity>> porHash) {
        return codigo == null || codigo.isBlank() ? Optional.empty() : porHash.apply(hash(codigo.trim()));
    }

    private static EstadoEnlace estado(Optional<EnlaceContrasenaEntity> enlace, Optional<UsuarioEntity> usuario,
                                       LocalDateTime ahora) {
        if (enlace.isEmpty() || usuario.isEmpty() || !Boolean.TRUE.equals(usuario.get().getActivo())) {
            return EstadoEnlace.INVALIDO;
        }
        EnlaceContrasenaEntity e = enlace.get();
        if (e.getFechaUso() != null) {
            return EstadoEnlace.USADO;
        }
        if (Boolean.TRUE.equals(e.getAnulado())) {
            return EstadoEnlace.ANULADO;
        }
        return ahora.isBefore(e.getVence()) ? EstadoEnlace.VIGENTE : EstadoEnlace.VENCIDO;
    }

    /** Lo que ve la página pública del enlace; sin datos del usuario si no está vigente. */
    public record ConsultaEnlace(EstadoEnlace estado, PropositoEnlace proposito, String nombre, String correo,
                                 LocalDateTime vence) {
    }

    /**
     * Los tokens emitidos antes de este momento dejan de servir (UsuarioJwtConverter). Truncado a
     * segundos porque el iat del token viene en segundos: así el token que se emite justo después no
     * queda rechazado por milisegundos.
     */
    static void cerrarSesiones(UsuarioEntity usuario, LocalDateTime ahora) {
        usuario.setSesionesDesde(ahora.truncatedTo(ChronoUnit.SECONDS));
    }

    private String nuevoCodigo() {
        byte[] bytes = new byte[BYTES_CODIGO];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal: así se guarda y así se busca el código que llega en el enlace. */
    static String hash(String codigo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(codigo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

}
