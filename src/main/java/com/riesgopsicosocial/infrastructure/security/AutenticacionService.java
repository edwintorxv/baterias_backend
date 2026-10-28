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
     * Mismo mensaje si el usuario no existe, está inactivo o la contraseña no coincide (401): no se
     * dice cuál de las tres falló. Con el usuario o la IP bloqueados responde 429 sin mirar la
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
        boolean coincide = passwordEncoder.matches(contrasena,
                encontrado.map(UsuarioEntity::getContrasenaHash).orElse(hashDeRelleno));
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

    public void cambiarContrasena(UsuarioAutenticado usuario, String actual, String nueva) {
        UsuarioEntity entidad = usuarioService.obtenerPorId(usuario.id());
        if (!passwordEncoder.matches(actual, entidad.getContrasenaHash())) {
            throw new ValidationException("La contraseña actual no es correcta");
        }
        if (actual.equals(nueva)) {
            throw new ValidationException("La contraseña nueva debe ser distinta de la actual");
        }
        usuarioService.cambiarContrasena(usuario.id(), nueva);
    }

    public record Sesion(TokenService.Token token, UsuarioAutenticado usuario) {
    }

}
