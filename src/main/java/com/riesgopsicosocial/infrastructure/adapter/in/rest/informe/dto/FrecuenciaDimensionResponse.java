package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * @param respuestas base de los porcentajes: personas × ítems
 */
public record FrecuenciaDimensionResponse(
        Long idDimensionCuestionario,
        String nombre,
        int personas,
        int items,
        int respuestas,
        List<ConteoFrecuenciaResponse> frecuencias
) {
}
