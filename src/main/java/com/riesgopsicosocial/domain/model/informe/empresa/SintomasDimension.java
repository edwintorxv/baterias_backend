package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * Síntomas (ítems) de una dimensión del cuestionario de estrés con la frecuencia de cada uno.
 *
 * @param items del síntoma más frecuente al menos ({@link FrecuenciaItem#masFrecuentePrimero()});
 *              empate: orden del cuestionario
 */
public record SintomasDimension(Long idDimensionCuestionario, String nombre, List<FrecuenciaItem> items) {
}
