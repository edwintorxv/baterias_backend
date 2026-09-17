package com.riesgopsicosocial.infrastructure.adapter.in.rest.baremodominio.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BaremoDominioRequest(
        @NotNull Long fkDominioCuestionario,
        @NotNull Long fkNivelRiesgo,
        @NotNull BigDecimal valorMinimo,
        @NotNull BigDecimal valorMaximo) {
}
