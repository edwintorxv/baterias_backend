package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluado.dto;

public record EvaluadoResponse(
        Long id,
        String numeroIdentificacion,
        String nombre,
        String apellido,
        Long fkSexo,
        Integer anioNacimiento
) {
}
