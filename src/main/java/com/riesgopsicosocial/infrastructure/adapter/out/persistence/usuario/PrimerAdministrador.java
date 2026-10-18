package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea el primer administrador al arrancar, solo si no hay ningún usuario. Usuario y contraseña
 * salen de variables de entorno ({@code APP_SEGURIDAD_ADMIN_USUARIO}, {@code APP_SEGURIDAD_ADMIN_CONTRASENA}),
 * nunca del código: sin ellas no crea nada y lo advierte en el log. Después de creado, las variables
 * ya no se usan (cambiar la contraseña es por la API).
 */
@Component
public class PrimerAdministrador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PrimerAdministrador.class);

    private final UsuarioJpaRepository repository;
    private final UsuarioService service;
    private final String usuario;
    private final String contrasena;

    public PrimerAdministrador(UsuarioJpaRepository repository, UsuarioService service,
                               @Value("${app.seguridad.admin.usuario:}") String usuario,
                               @Value("${app.seguridad.admin.contrasena:}") String contrasena) {
        this.repository = repository;
        this.service = service;
        this.usuario = usuario;
        this.contrasena = contrasena;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        if (usuario.isBlank() || contrasena.isBlank()) {
            log.warn("No hay usuarios y no se definieron APP_SEGURIDAD_ADMIN_USUARIO / APP_SEGURIDAD_ADMIN_CONTRASENA: "
                    + "no se creó el primer administrador");
            return;
        }
        if (contrasena.length() < 8) {
            log.warn("APP_SEGURIDAD_ADMIN_CONTRASENA debe tener al menos 8 caracteres: no se creó el primer administrador");
            return;
        }
        UsuarioEntity admin = new UsuarioEntity();
        admin.setUsuario(usuario);
        admin.setNombre("Administrador");
        admin.setRol(Rol.ADMIN);
        service.crear(admin, contrasena);
        log.info("Primer administrador creado: {}", usuario);
    }

}
