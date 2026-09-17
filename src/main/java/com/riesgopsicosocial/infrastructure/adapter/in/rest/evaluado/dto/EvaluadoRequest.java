package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EvaluadoRequest(
        @NotBlank @Size(max = 30) String numeroIdentificacion,
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 150) String apellido,
        Long fkSexo,
        Integer anioNacimiento,
        Long fkEstadoCivil,
        Long fkNivelAcademico,
        Long fkCiudadResidencia,
        Long fkEstratoSocioeconomico,
        Long fkTipoVivienda,
        @Size(max = 200) String ocupacionProfesion,
        Integer familiaresDependientesEconomicamente
) {
}
