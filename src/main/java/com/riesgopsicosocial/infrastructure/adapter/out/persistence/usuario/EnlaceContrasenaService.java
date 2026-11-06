package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import com.riesgopsicosocial.infrastructure.security.UsuarioActual;
import com.riesgopsicosocial.shared.exception.BusinessException;
import com.riesgopsicosocial.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public EnlaceContrasenaService(UsuarioJpaRepository usuarioRepository,
                                   EnlaceContrasenaJpaRepository enlaceRepository,
                                   ApplicationEventPublisher eventos) {
        this(usuarioRepository, enlaceRepository, eventos, Clock.systemDefaultZone());
    }

    EnlaceContrasenaService(UsuarioJpaRepository usuarioRepository, EnlaceContrasenaJpaRepository enlaceRepository,
                            ApplicationEventPublisher eventos, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.enlaceRepository = enlaceRepository;
        this.eventos = eventos;
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
        UsuarioEntity usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + idUsuario));
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
