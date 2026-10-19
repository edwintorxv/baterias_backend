package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto;

import java.time.LocalDate;

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
        Long fkEstadoCivil,
        Long fkNivelAcademico,
        Long fkCiudadResidencia,
        Long fkEstratoSocioeconomico,
        Long fkTipoVivienda,
        String ocupacionProfesion,
        Integer familiaresDependientesEconomicamente,
        LocalDate fechaVinculacion,
        LocalDate fechaRetiro,
        Boolean activo
) {
}
