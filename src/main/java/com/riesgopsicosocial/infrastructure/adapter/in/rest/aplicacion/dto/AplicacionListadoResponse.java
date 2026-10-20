package com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion.dto;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta.EstadoAplicacion;

import java.time.LocalDateTime;

/** Fila del listado general de aplicaciones; {@code estado} con la misma regla del formulario. */
public record AplicacionListadoResponse(

        Long id,
        LocalDateTime fechaAplicacion,
        Long idCliente,
        String cliente,
        Long idRelacion,
        Boolean relacionActiva,
        Long idEvaluado,
        String numeroIdentificacion,
        String nombre,
        String apellido,
        String forma,
        Long idEvaluador,
        String evaluador,
        int aplicables,
        int respondidas,
        EstadoAplicacion estado

) {
}
