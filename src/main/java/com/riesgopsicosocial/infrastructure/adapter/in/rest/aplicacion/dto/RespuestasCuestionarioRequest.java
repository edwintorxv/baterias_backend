package com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Respuestas de un cuestionario completo (o a medias): reemplazan las que tenía. Lo que no venga queda
 * sin responder.
 */
public record RespuestasCuestionarioRequest(
        @NotNull List<@NotNull @Valid Respuesta> respuestas
) {

    public record Respuesta(
            @NotNull Long fkPregunta,
            @NotNull Long fkOpcionRespuesta
    ) {
    }

}
