package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Una persona con su vinculación a un cliente (formulario o fila del archivo). Obligatorios: documento,
 * nombre, apellido, sexo, año de nacimiento, nivel académico (población del informe) y tipo de cargo
 * (de él sale la forma A o B).
 *
 * @param fechaVinculacion {@code null} = hoy; si la persona ya estaba activa en este cliente, se conserva la que tenía
 */
public record VincularPersonaRequest(
        // La persona (si ya existe, ganan estos datos)
        @NotBlank(message = "El número de identificación es obligatorio")
        @Size(max = 30, message = "El número de identificación admite máximo 30 caracteres")
        @Pattern(regexp = "\\s*[0-9A-Za-z-]+\\s*", message = "El número de identificación solo admite letras, números y guion")
        String numeroIdentificacion,
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres") String nombre,
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 150, message = "El apellido admite máximo 150 caracteres") String apellido,
        @NotNull(message = "El sexo es obligatorio") Long fkSexo,
        @NotNull(message = "El año de nacimiento es obligatorio") Integer anioNacimiento,

        // Datos de la persona que cambian (foto de esta vinculación)
        Long fkEstadoCivil,
        @NotNull(message = "El nivel académico es obligatorio") Long fkNivelAcademico,
        Long fkCiudadResidencia,
        Long fkEstratoSocioeconomico,
        Long fkTipoVivienda,
        @Size(max = 200, message = "La ocupación o profesión admite máximo 200 caracteres") String ocupacionProfesion,
        @PositiveOrZero(message = "Las personas a cargo no pueden ser negativas") Integer familiaresDependientesEconomicamente,

        // Datos laborales
        Long fkCiudadTrabajo,
        @Size(max = 100, message = "La antigüedad admite máximo 100 caracteres") String antiguedadEmpresa,
        @Size(max = 200, message = "El cargo admite máximo 200 caracteres") String nombreCargo,
        @NotNull(message = "El tipo de cargo es obligatorio") Long fkTipoCargo,
        Long fkTiempoCargo,
        @Size(max = 200, message = "El área admite máximo 200 caracteres") String nombreArea,
        Long fkTipoContrato,
        Long fkHorasLabor,
        Long fkTipoSalario,

        LocalDate fechaVinculacion
) {
}
