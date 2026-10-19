package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto;

/**
 * @param resultado                NUEVA, VINCULA, ACTUALIZA o CAMBIA_EMPRESA
 * @param datosPersonaActualizados la persona ya existía y se actualizaron nombre, apellido, sexo o año
 * @param relacion                 la relación activa con este cliente (nueva o actualizada)
 */
public record VinculacionResponse(
        String resultado,
        Long idEvaluado,
        boolean datosPersonaActualizados,
        EvaluadoClienteResponse relacion
) {
}
