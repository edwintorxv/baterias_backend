package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.math.BigDecimal;

public record NivelFormaResponse(
        Long idCuestionario,
        String forma,
        int personas,
        BigDecimal promedioPuntajeBruto,
        BigDecimal puntajeTransformado,
        Long fkNivelRiesgo,
        String nivel
) {
}
