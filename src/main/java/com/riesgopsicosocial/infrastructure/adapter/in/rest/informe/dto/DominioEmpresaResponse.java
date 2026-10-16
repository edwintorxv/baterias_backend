package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

public record DominioEmpresaResponse(
        Long idDominioCuestionario,
        String nombre,
        DistribucionNivelesResponse distribucion,
        List<DistribucionItemResponse> dimensiones
) {
}
