package com.riesgopsicosocial.infrastructure.adapter.in.rest.pregunta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PreguntaRequest(
        @NotNull Long fkDimensionCuestionario,
        @NotNull Long fkEscala,
        @NotNull Integer numero,
        @NotBlank String descripcion) {
}
