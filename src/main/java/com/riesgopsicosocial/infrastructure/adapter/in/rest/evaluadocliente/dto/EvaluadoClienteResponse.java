package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto;

public record EvaluadoClienteResponse(
        Long id,
        Long fkEvaluado,
        Long fkCliente,
        Long fkCiudadTrabajo,
        String antiguedadEmpresa,
        String nombreCargo,
        Long fkTipoCargo,
        Long fkTiempoCargo,
        String nombreArea,
        Long fkTipoContrato,
        Long fkHorasLabor,
        Long fkTipoSalario,
        Boolean activo
) {
}
