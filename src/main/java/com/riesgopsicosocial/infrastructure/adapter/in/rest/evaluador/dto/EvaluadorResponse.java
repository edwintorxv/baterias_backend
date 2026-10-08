package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluador.dto;

import java.time.LocalDate;

/** La imagen de la firma no viaja aquí: {@code tieneFirma} indica si existe y se consulta en /firma. */
public record EvaluadorResponse(
        Long id,
        String numeroIdentificacion,
        String nombre,
        String profesion,
        String posgrado,
        String tarjetaProfesional,
        String licenciaSaludOcupacional,
        LocalDate fechaExpedicionLicencia,
        Boolean activo,
        boolean tieneFirma
) {
}
