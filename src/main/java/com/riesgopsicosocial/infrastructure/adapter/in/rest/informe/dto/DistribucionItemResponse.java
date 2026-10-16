package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

/**
 * Distribución por nivel de un dominio o una dimensión.
 *
 * @param id id de la dimensión del cuestionario ({@code dimension_cuestionario}) o, en los dominios con
 *           las formas A y B juntas, id del dominio
 */
public record DistribucionItemResponse(
        Long id,
        String nombre,
        DistribucionNivelesResponse distribucion
) {
}
