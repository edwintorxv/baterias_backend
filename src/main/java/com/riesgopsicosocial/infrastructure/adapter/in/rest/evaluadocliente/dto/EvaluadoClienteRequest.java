package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EvaluadoClienteRequest(
        @NotNull Long fkEvaluado,
        @NotNull Long fkCliente,
        Long fkCiudadTrabajo,
        @Size(max = 100) String antiguedadEmpresa,
        @Size(max = 200) String nombreCargo,
        Long fkTipoCargo,
        Long fkTiempoCargo,
        @Size(max = 200) String nombreArea,
        Long fkTipoContrato,
        Long fkHorasLabor,
        Long fkTipoSalario,
        Boolean activo) {
}
