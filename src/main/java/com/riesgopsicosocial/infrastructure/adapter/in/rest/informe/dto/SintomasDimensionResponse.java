package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * @param items síntomas de la dimensión, del más frecuente al menos (primero el de mayor % en "Siempre")
 */
public record SintomasDimensionResponse(
        Long idDimensionCuestionario,
        String nombre,
        List<FrecuenciaItemResponse> items
) {
}
