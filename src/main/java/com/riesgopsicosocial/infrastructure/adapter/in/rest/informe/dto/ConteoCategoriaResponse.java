package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.math.BigDecimal;

public record ConteoCategoriaResponse(
        String categoria,
        int cantidad,
        BigDecimal porcentaje
) {
}
