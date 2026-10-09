package com.riesgopsicosocial.domain.model.resultado;

import java.math.BigDecimal;

/**
 * @param idDominioCuestionario dominio al que pertenece (A/B); {@code null} en cuestionarios sin dominios
 */
public record ResultadoDimension(
        Long idDimensionCuestionario,
        Long idDominioCuestionario,
        String nombre,
        BigDecimal puntajeBruto,
        BigDecimal puntajeTransformado,
        NivelRiesgo nivelRiesgo
) {
}
