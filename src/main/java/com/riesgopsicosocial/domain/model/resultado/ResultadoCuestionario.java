package com.riesgopsicosocial.domain.model.resultado;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado total de un cuestionario con el detalle que aplique según su método:
 * A/B traen dominios y dimensiones, C solo dimensiones y D solo el total
 * (el manual no define baremos por dimensión para estrés).
 */
public record ResultadoCuestionario(
        Long idCuestionario,
        String forma,
        BigDecimal puntajeBruto,
        BigDecimal puntajeTransformado,
        NivelRiesgo nivelRiesgo,
        List<ResultadoDominio> dominios,
        List<ResultadoDimension> dimensiones
) {
}
