package com.riesgopsicosocial.infrastructure.adapter.in.rest.aplicacion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AplicacionRequest(

        @NotNull Long fkEvaluadoCliente,
        LocalDateTime fechaAplicacion,
        String observaciones,
        @Size(max = 20) String estado

) {
}
