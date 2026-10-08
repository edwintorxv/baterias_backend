package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

public record InformeEvaluadoResponse(

        ClienteInformeResponse cliente,
        EvaluadoInformeResponse evaluado,
        List<AplicacionInformeResponse> aplicaciones

) {
}
