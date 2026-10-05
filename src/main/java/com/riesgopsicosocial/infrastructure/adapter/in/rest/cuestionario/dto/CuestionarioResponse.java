package com.riesgopsicosocial.infrastructure.adapter.in.rest.cuestionario.dto;

import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;

import java.math.BigDecimal;

public record CuestionarioResponse(
        Long id,
        String forma,
        String descripcion,
        BigDecimal factorTransformacion,
        MetodoCalculo metodoCalculo
) {
}
