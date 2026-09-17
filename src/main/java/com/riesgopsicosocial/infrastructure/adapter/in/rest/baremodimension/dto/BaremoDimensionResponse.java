package com.riesgopsicosocial.infrastructure.adapter.in.rest.baremodimension.dto;

import java.math.BigDecimal;

public record BaremoDimensionResponse(
        Long id,
        Long fkDimensionCuestionario,
        Long fkNivelRiesgo,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo) {
}
