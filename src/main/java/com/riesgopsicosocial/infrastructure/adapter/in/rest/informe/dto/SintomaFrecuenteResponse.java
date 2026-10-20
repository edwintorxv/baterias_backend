package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

/**
 * @param numero     número de la pregunta en el cuestionario de estrés (referencia)
 * @param frecuencia Siempre o Casi siempre
 */
public record SintomaFrecuenteResponse(
        int numero,
        String descripcion,
        String frecuencia
) {
}
