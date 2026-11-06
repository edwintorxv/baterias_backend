package com.riesgopsicosocial.infrastructure.adapter.out.persistence.usuario;

import java.time.LocalDateTime;

/**
 * Se publica al generar un enlace; el correo sale después del commit (si la transacción falla, no se
 * envía nada). El código en claro solo viaja en este evento, en memoria, hasta el correo.
 */
public record EnlaceContrasenaGenerado(String correo, String nombre, PropositoEnlace proposito,
                                       String codigo, LocalDateTime vence) {

    @Override
    public String toString() {
        return "EnlaceContrasenaGenerado[correo=" + correo + ", proposito=" + proposito + ", vence=" + vence + "]";
    }

}
