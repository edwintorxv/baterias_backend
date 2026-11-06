package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso.ResultadoIngreso;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioService;
import com.riesgopsicosocial.shared.exception.DemasiadosIntentosException;
import com.riesgopsicosocial.shared.exception.ValidationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Inicio de sesión y cambio de la propia contraseña. */
@Service
public class AutenticacionService {

    static final String CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos";

    private final UsuarioJpaRepository repository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LimiteIntentosLogin limiteIntentos;
    private final RegistroIngresos registroIngresos;
    /** Para comparar aunque el usuario no exista: así tarda lo mismo y no revela qué usuarios hay. */
    private final String hashDeRelleno;

    public AutenticacionService(UsuarioJpaRepository repository, UsuarioService usuarioService,
                                PasswordEncoder passwordEncoder, TokenService tokenService,
                                LimiteIntentosLogin limiteIntentos, RegistroIngresos registroIngresos) {
        this.repository = repository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.limiteIntentos = limiteIntentos;
        this.registroIngresos = registroIngresos;
        this.hashDeRelleno = passwordEncoder.encode("relleno-para-usuarios-inexistentes");
    }

    /**
     * Mismo mensaje si el usuario no existe, está inactivo, aún no tiene contraseña o no coincide (401):
     * no se dice cuál falló. Con el usuario o la IP bloqueados responde 429 sin mirar la
     * contraseña, aunque sea la correcta. Cada intento queda en registro_ingreso (el bloqueo, solo el
     * primer rechazo).
     */
    public Sesion iniciarSesion(String correo, String contrasena, String ip, String navegador) {
        String usuario = UsuarioService.normalizarCorreo(correo);
        try {
            limiteIntentos.verificar(usuario, ip);
        } catch (DemasiadosIntentosException e) {
            if (limiteIntentos.primerRechazoDelBloqueo(usuario, ip)) {
                registroIngresos.registrar(usuario, null, ip, navegador, ResultadoIngreso.BLOQUEADO);
            }
            throw e;
        }
        Optional<UsuarioEntity> encontrado = repository.findByCorreo(usuario);
        // Sin usuario o sin contraseña (aún no la define por el enlace) se compara contra el relleno para
        // tardar lo mismo, pero nunca coincide: ni siquiera escribiendo el texto del relleno.
        String hash = encontrado.map(UsuarioEntity::getContrasenaHash).orElse(null);
        boolean coincide = passwordEncoder.matches(contrasena, hash != null ? hash : hashDeRelleno) && hash != null;
        Optional<UsuarioEntity> valido = encontrado.filter(u -> coincide && Boolean.TRUE.equals(u.getActivo()));
        Long idUsuario = encontrado.map(UsuarioEntity::getId).orElse(null);
        if (valido.isEmpty()) {
            limiteIntentos.registrarFallo(usuario, ip);
            registroIngresos.registrar(usuario, idUsuario, ip, navegador, ResultadoIngreso.FALLIDO);
            throw new BadCredentialsException(CREDENCIALES_INVALIDAS);
        }
        limiteIntentos.registrarExito(usuario);
        registroIngresos.registrar(usuario, idUsuario, ip, navegador, ResultadoIngreso.EXITOSO);
        UsuarioEntity entidad = valido.get();
        return new Sesion(tokenService.emitir(entidad), UsuarioAutenticado.de(entidad));
    }

    /**
     * Cambia la propia contraseña: cierra las sesiones de los demás equipos y devuelve un token nuevo para
     * que la actual siga abierta. La contraseña actual cuenta para el mismo freno del login (por correo y
     * por IP): adivinarla aquí o en el login suma al mismo bloqueo (429).
     */
    public Sesion cambiarContrasena(UsuarioAutenticado usuario, String actual, String nueva, String ip) {
        String correo = UsuarioService.normalizarCorreo(usuario.correo());
        limiteIntentos.verificar(correo, ip);
        UsuarioEntity entidad = usuarioService.obtenerPorId(usuario.id());
        String hash = entidad.getContrasenaHash();
        if (hash == null || !passwordEncoder.matches(actual, hash)) {
            limiteIntentos.registrarFallo(correo, ip);
            throw new ValidationException("La contraseña actual no es correcta");
        }
        limiteIntentos.registrarExito(correo);
        if (actual.equals(nueva)) {
            throw new ValidationException("La contraseña nueva debe ser distinta de la actual");
        }
        UsuarioEntity actualizado = usuarioService.cambiarContrasena(usuario.id(), nueva);
        return new Sesion(tokenService.emitir(actualizado), UsuarioAutenticado.de(actualizado));
    }

    public record Sesion(TokenService.Token token, UsuarioAutenticado usuario) {
    }

}
