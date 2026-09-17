package com.riesgopsicosocial.infrastructure.adapter.in.rest.cuestionario.dto;

import jakarta.validation.constraints.NotBlank;

public record CuestionarioRequest(String forma, @NotBlank String descripcion
) {
}
