package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * @param dominios ranking, del más crítico al menos crítico, cada uno con el ranking de sus dimensiones
 */
public record FormaIntralaboralResponse(
        Long idCuestionario,
        String forma,
        int personas,
        List<DominioEmpresaResponse> dominios
) {
}
