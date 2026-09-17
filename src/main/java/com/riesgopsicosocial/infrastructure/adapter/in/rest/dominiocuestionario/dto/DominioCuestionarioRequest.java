package com.riesgopsicosocial.infrastructure.adapter.in.rest.dominiocuestionario.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DominioCuestionarioRequest(
        @NotNull Long fkDominio,
        @NotNull Long fkCuestionario,
        @NotNull BigDecimal factorTransformacion) {
}
