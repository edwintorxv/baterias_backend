package com.riesgopsicosocial.infrastructure.adapter.in.rest.cuestionario.dto;

import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CuestionarioRequest(
        String forma,
        @NotBlank String descripcion,
        @NotNull BigDecimal factorTransformacion,
        @NotNull MetodoCalculo metodoCalculo
) {
}
