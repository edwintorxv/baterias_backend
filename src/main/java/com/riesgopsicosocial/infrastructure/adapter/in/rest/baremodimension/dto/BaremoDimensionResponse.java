package com.riesgopsicosocial.infrastructure.adapter.in.rest.baremodimension.dto;

import java.math.BigDecimal;

public record BaremoDimensionResponse(
        Long id,
        Long fkDimensionCuestionario,
        Long fkNivelRiesgo,
        Long fkGrupoOcupacional,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo) {
}
