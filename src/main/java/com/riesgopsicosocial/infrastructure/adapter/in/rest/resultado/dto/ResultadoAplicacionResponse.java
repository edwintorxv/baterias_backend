package com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ResultadoAplicacionResponse(

        Long fkAplicacion,
        Long fkGrupoOcupacional,
        LocalDateTime fechaCalculo,
        List<ResultadoCuestionarioResponse> cuestionarios,
        ResultadoTotalGeneralResponse totalGeneral

) {
}
