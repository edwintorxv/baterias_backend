package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * @param personas    personas con resultado de estrés
 * @param dimensiones frecuencia de síntomas por dimensión (el manual no trae niveles por dimensión)
 * @param porItem     cada dimensión, en el orden del cuestionario, con la frecuencia de cada uno de sus síntomas
 * @param porCiudad   nivel total de síntomas de estrés por ciudad de trabajo (etiquetas del manual de estrés)
 * @param porArea     nivel total de síntomas de estrés por área (etiquetas del manual de estrés)
 */
public record SintomasEstresResponse(
        int personas,
        List<FrecuenciaDimensionResponse> dimensiones,
        List<SintomasDimensionResponse> porItem,
        DesgloseGruposResponse porCiudad,
        DesgloseGruposResponse porArea
) {
}
