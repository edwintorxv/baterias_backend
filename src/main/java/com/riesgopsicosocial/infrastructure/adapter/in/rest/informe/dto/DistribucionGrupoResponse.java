package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

public record DistribucionGrupoResponse(
        String grupo,
        DistribucionNivelesResponse distribucion
) {
}
