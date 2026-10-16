package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * @param personas    personas con resultado de estrés
 * @param dimensiones frecuencia de síntomas por dimensión (el manual no trae niveles por dimensión)
 * @param porCiudad   nivel total de síntomas de estrés por ciudad de trabajo (etiquetas del manual de estrés)
 * @param porArea     nivel total de síntomas de estrés por área (etiquetas del manual de estrés)
 */
public record SintomasEstresResponse(
        int personas,
        List<FrecuenciaDimensionResponse> dimensiones,
        DesgloseGruposResponse porCiudad,
        DesgloseGruposResponse porArea
) {
}
