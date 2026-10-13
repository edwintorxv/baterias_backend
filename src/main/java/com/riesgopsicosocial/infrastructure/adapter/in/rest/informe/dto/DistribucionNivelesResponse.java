package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

public record DistribucionNivelesResponse(

        int total,
        List<ConteoNivelResponse> niveles

) {
}
