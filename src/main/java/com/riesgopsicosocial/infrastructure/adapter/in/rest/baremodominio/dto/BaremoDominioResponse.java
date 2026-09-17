package com.riesgopsicosocial.infrastructure.adapter.in.rest.baremodominio.dto;

import java.math.BigDecimal;

public record BaremoDominioResponse(
        Long id,
        Long fkDominioCuestionario,
        Long fkNivelRiesgo,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo) {
}
