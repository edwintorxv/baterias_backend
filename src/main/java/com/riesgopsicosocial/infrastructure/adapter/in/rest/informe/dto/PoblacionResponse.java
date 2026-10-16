package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * @param personas personas evaluadas: el total de cada variable
 */
public record PoblacionResponse(
        int personas,
        List<VariablePoblacionResponse> variables
) {
}
