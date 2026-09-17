package com.riesgopsicosocial.infrastructure.adapter.in.rest.pregunta.dto;

public record PreguntaResponse(
        Long id,
        Long fkDimensionCuestionario,
        Long fkEscala,
        Integer numero,
        String descripcion) {
}
