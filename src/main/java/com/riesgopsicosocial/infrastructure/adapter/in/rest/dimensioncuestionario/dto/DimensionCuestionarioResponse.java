package com.riesgopsicosocial.infrastructure.adapter.in.rest.dimensioncuestionario.dto;

import java.math.BigDecimal;

public record DimensionCuestionarioResponse(
        Long id,
        Long fkDimension,
        Long fkCuestionario,
        BigDecimal factorTransformacion) {
}
