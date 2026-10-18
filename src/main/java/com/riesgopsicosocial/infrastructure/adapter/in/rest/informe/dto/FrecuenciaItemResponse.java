package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * @param numero   número de la pregunta en el cuestionario (el Word no lo muestra: los ítems van ordenados por frecuencia)
 * @param personas personas que respondieron el ítem, base de los porcentajes
 */
public record FrecuenciaItemResponse(
        Long idPregunta,
        int numero,
        String descripcion,
        int personas,
        List<ConteoFrecuenciaResponse> frecuencias
) {
}
