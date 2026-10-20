package com.riesgopsicosocial.domain.model.informe;

/**
 * Síntoma del cuestionario de estrés que el trabajador reportó con frecuencia (Siempre o Casi siempre).
 *
 * @param numero     número de la pregunta en el cuestionario
 * @param frecuencia la opción que respondió
 */
public record SintomaFrecuente(int numero, String descripcion, String frecuencia) {
}
