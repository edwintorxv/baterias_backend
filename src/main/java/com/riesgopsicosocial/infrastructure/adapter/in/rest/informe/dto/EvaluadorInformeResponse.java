package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.time.LocalDate;

public record EvaluadorInformeResponse(

        Long id,
        String numeroIdentificacion,
        String nombre,
        String profesion,
        String posgrado,
        String tarjetaProfesional,
        String licenciaSaludOcupacional,
        LocalDate fechaExpedicionLicencia,
        boolean tieneFirma

) {
}
