package com.riesgopsicosocial.infrastructure.adapter.in.rest.baremodimension.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BaremoDimensionRequest(
        @NotNull Long fkDimensionCuestionario,
        @NotNull Long fkNivelRiesgo,
        @NotNull BigDecimal valorMinimo,
        @NotNull BigDecimal valorMaximo) {
}
