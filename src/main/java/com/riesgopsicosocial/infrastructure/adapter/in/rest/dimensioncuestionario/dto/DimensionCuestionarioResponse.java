package com.riesgopsicosocial.infrastructure.adapter.in.rest.dimensioncuestionario.dto;

import com.riesgopsicosocial.domain.model.resultado.configuracion.CondicionAplicacion;

import java.math.BigDecimal;

public record DimensionCuestionarioResponse(
        Long id,
        Long fkDimension,
        Long fkCuestionario,
        BigDecimal factorTransformacion,
        BigDecimal peso,
        CondicionAplicacion condicionAplicacion,
        Short maxItemsSinRespuesta) {
}
