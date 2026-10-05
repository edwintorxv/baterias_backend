package com.riesgopsicosocial.domain.model.resultado;

import java.math.BigDecimal;

public record ResultadoDominio(
        Long idDominioCuestionario,
        String nombre,
        BigDecimal puntajeBruto,
        BigDecimal puntajeTransformado,
        NivelRiesgo nivelRiesgo
) {
}
