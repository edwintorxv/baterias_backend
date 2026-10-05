package com.riesgopsicosocial.domain.model.resultado;

import java.math.BigDecimal;

public record ResultadoDimension(
        Long idDimensionCuestionario,
        String nombre,
        BigDecimal puntajeBruto,
        BigDecimal puntajeTransformado,
        NivelRiesgo nivelRiesgo
) {
}
