package com.riesgopsicosocial.infrastructure.adapter.in.rest.dimensioncuestionario.dto;

import com.riesgopsicosocial.domain.model.resultado.configuracion.CondicionAplicacion;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DimensionCuestionarioRequest(
        @NotNull Long fkDimension,
        @NotNull Long fkCuestionario,
        @NotNull BigDecimal factorTransformacion,
        BigDecimal peso,
        CondicionAplicacion condicionAplicacion,
        @Min(0) Short maxItemsSinRespuesta) {
}
