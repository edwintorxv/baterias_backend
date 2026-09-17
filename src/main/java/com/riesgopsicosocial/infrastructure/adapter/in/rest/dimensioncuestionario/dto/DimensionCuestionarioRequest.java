package com.riesgopsicosocial.infrastructure.adapter.in.rest.dimensioncuestionario.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DimensionCuestionarioRequest(
        @NotNull Long fkDimension,
        @NotNull Long fkCuestionario,
        @NotNull BigDecimal factorTransformacion) {
}
