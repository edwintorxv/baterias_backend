package com.riesgopsicosocial.infrastructure.adapter.in.rest.baremodominio.dto;

import java.math.BigDecimal;

public record BaremoDominioResponse(
        Long id,
        Long fkDominioCuestionario,
        Long fkNivelRiesgo,
        Long fkGrupoOcupacional,
        BigDecimal valorMinimo,
        BigDecimal valorMaximo) {
}
