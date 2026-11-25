package com.riesgopsicosocial.infrastructure.adapter.out.persistence.registroingreso;

import java.time.LocalDateTime;

/** Un intento de ingreso para el listado del administrador, con el nombre del usuario si el correo existe. */
public record IngresoFila(
        Long id,
        LocalDateTime fecha,
        String correo,
        Long idUsuario,
        String nombreUsuario,
        String ip,
        String navegador,
        ResultadoIngreso resultado
) {
}
