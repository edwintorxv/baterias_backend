package com.riesgopsicosocial.infrastructure.adapter.in.rest.dimension.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DimensionRequest(
        @NotNull(message = "El dominio es obligatorio")
        Long fkDominio,

        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion
) {
}
