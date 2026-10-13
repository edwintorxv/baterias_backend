package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

public record InformeEmpresaResponse(

        ClienteInformeResponse cliente,
        int anio,
        CoberturaResponse cobertura,
        List<CoberturaCiudadResponse> coberturaPorCiudad,
        int pendientesCalculo,
        DistribucionNivelesResponse intralaboral,
        DistribucionNivelesResponse extralaboral,
        DistribucionNivelesResponse estres

) {
}
