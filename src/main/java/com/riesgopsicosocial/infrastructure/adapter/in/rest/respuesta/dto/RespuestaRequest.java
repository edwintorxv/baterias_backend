package com.riesgopsicosocial.infrastructure.adapter.in.rest.respuesta.dto;

import jakarta.validation.constraints.NotNull;

public record RespuestaRequest(

        @NotNull Long fkAplicacion,
        @NotNull Long fkPregunta,
        @NotNull Long fkOpcionRespuesta

) {
}
