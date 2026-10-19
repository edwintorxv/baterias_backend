package com.riesgopsicosocial.infrastructure.security;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioEntity;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioJpaRepository;
import com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario.UsuarioService;
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
    /** Para comparar aunque el usuario no exista: así tarda lo mismo y no revela qué usuarios hay. */
    private final String hashDeRelleno;

    public AutenticacionService(UsuarioJpaRepository repository, UsuarioService usuarioService,
                                PasswordEncoder passwordEncoder, TokenService tokenService,
                                LimiteIntentosLogin limiteIntentos) {
        this.repository = repository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.limiteIntentos = limiteIntentos;
        this.hashDeRelleno = passwordEncoder.encode("relleno-para-usuarios-inexistentes");
    }

    /**
     * Mismo mensaje si el usuario no existe, está inactivo o la contraseña no coincide (401): no se
     * dice cuál de las tres falló. Con el usuario o la IP bloqueados responde 429 sin mirar la
     * contraseña, aunque sea la correcta.
     */
    public Sesion iniciarSesion(String usuario, String contrasena, String ip) {
        limiteIntentos.verificar(usuario, ip);
        Optional<UsuarioEntity> encontrado = repository.findByUsuarioIgnoreCase(usuario.trim());
        boolean coincide = passwordEncoder.matches(contrasena,
                encontrado.map(UsuarioEntity::getContrasenaHash).orElse(hashDeRelleno));
        Optional<UsuarioEntity> valido = encontrado.filter(u -> coincide && Boolean.TRUE.equals(u.getActivo()));
        if (valido.isEmpty()) {
            limiteIntentos.registrarFallo(usuario, ip);
            throw new BadCredentialsException(CREDENCIALES_INVALIDAS);
        }
        limiteIntentos.registrarExito(usuario);
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
