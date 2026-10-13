package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.math.BigDecimal;

public record CoberturaResponse(

        int esperados,
        int evaluados,
        BigDecimal porcentaje

) {
}
