package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import java.time.LocalDateTime;

/** Estado de la contraseña de un usuario y, si está pendiente, cuándo vence su enlace. Nunca la contraseña. */
public record ContrasenaUsuario(EstadoContrasena estado, LocalDateTime venceEnlace) {

    static final ContrasenaUsuario DEFINIDA = new ContrasenaUsuario(EstadoContrasena.DEFINIDA, null);
    static final ContrasenaUsuario SIN_ENLACE = new ContrasenaUsuario(EstadoContrasena.SIN_ENLACE, null);

    static ContrasenaUsuario pendiente(LocalDateTime vence) {
        return new ContrasenaUsuario(EstadoContrasena.PENDIENTE, vence);
    }
}
