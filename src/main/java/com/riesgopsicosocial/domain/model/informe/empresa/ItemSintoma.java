package com.riesgopsicosocial.domain.model.informe.empresa;

/**
 * Ítem (pregunta) del cuestionario de estrés: un síntoma, con su dimensión.
 *
 * @param numero número de la pregunta en el cuestionario
 */
public record ItemSintoma(Long idPregunta, Long idDimensionCuestionario, int numero, String descripcion) {
}
