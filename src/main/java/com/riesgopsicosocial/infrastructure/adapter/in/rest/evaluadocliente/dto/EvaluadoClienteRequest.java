package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos laborales y los datos de la persona que cambian con el tiempo (foto de esta vinculación).
 *
 * @param fechaVinculacion {@code null} = hoy al crear; al actualizar, se conserva la que tenía
 * @param fechaRetiro      solo cuenta si la relación queda inactiva; {@code null} = hoy
 */
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
        Long fkEstadoCivil,
        Long fkNivelAcademico,
        Long fkCiudadResidencia,
        Long fkEstratoSocioeconomico,
        Long fkTipoVivienda,
        @Size(max = 200) String ocupacionProfesion,
        @PositiveOrZero Integer familiaresDependientesEconomicamente,
        LocalDate fechaVinculacion,
        LocalDate fechaRetiro,
        Boolean activo) {
}
