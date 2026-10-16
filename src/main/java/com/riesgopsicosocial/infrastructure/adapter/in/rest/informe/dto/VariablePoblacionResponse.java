package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

public record VariablePoblacionResponse(
        String nombre,
        List<ConteoCategoriaResponse> categorias
) {
}
