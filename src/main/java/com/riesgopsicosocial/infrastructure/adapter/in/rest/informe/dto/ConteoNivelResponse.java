package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.math.BigDecimal;

public record ConteoNivelResponse(

        Long fkNivelRiesgo,
        String nivel,
        int cantidad,
        BigDecimal porcentaje

) {
}
