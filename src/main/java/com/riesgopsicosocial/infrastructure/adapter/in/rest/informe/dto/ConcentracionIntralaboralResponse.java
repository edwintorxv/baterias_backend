package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * Formas A y B juntas.
 *
 * @param dominios  ranking de los dominios (id = id del dominio)
 * @param porCiudad nivel intralaboral total por ciudad de trabajo
 * @param porArea   nivel intralaboral total por área
 */
public record ConcentracionIntralaboralResponse(
        List<DistribucionItemResponse> dominios,
        DesgloseGruposResponse porCiudad,
        DesgloseGruposResponse porArea
) {
}
