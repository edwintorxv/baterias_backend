package com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion.dto;

import java.time.LocalDateTime;

public record AplicacionResponse(

        Long id,
        Long fkEvaluadoCliente,
        LocalDateTime fechaAplicacion,
        String observaciones,
        String estado

) {
}
