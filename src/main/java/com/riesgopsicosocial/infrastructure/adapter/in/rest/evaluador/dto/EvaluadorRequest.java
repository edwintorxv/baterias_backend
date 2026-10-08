package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EvaluadorRequest(
        @NotBlank @Size(max = 30) String numeroIdentificacion,
        @NotBlank @Size(max = 200) String nombre,
        @NotBlank @Size(max = 200) String profesion,
        @Size(max = 200) String posgrado,
        @NotBlank @Size(max = 50) String tarjetaProfesional,
        @NotBlank @Size(max = 50) String licenciaSaludOcupacional,
        @NotNull LocalDate fechaExpedicionLicencia,
        Boolean activo
) {
}
