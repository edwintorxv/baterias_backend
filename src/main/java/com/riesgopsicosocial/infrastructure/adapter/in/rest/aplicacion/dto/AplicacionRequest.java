package com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AplicacionRequest(

        @NotNull Long fkEvaluadoCliente,
        Long fkGrupoOcupacional,
        LocalDateTime fechaAplicacion,
        String observaciones,
        String recomendaciones,
        Boolean atiendeClientes,
        Boolean esJefe,
        Long fkEvaluador

) {
}
