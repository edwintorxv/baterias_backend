package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.math.BigDecimal;

public record CoberturaCiudadResponse(

        Long idCiudad,
        String ciudad,
        int esperados,
        int evaluados,
        BigDecimal porcentaje

) {
}
