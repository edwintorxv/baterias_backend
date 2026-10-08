package com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion.dto;

import java.time.LocalDateTime;

public record AplicacionResponse(

        Long id,
        Long fkEvaluadoCliente,
        Long fkGrupoOcupacional,
        LocalDateTime fechaAplicacion,
        String observaciones,
        String recomendaciones,
        String estado,
        Boolean atiendeClientes,
        Boolean esJefe,
        Long fkEvaluador

) {
}
