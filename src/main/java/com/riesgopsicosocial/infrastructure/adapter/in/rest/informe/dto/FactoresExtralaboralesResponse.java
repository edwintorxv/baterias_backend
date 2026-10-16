package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * @param dimensiones ranking de las dimensiones extralaborales
 * @param porCiudad   nivel extralaboral total por ciudad de trabajo
 * @param porArea     nivel extralaboral total por área
 */
public record FactoresExtralaboralesResponse(
        List<DistribucionItemResponse> dimensiones,
        DesgloseGruposResponse porCiudad,
        DesgloseGruposResponse porArea
) {
}
