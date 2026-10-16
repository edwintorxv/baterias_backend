package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.math.BigDecimal;

public record ConteoFrecuenciaResponse(
        Long idOpcion,
        String opcion,
        int cantidad,
        BigDecimal porcentaje
) {
}
