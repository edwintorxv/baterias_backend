package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/** Dimensión del cuestionario de estrés con los síntomas reportados con frecuencia (vacía si ninguno). */
public record GrupoSintomasResponse(
        String nombre,
        List<SintomaFrecuenteResponse> sintomas
) {
}
