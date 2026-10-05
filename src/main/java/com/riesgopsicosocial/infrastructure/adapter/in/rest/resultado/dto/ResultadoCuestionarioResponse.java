package com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoCuestionarioResponse(

        Long fkCuestionario,
        String forma,
        BigDecimal puntajeBruto,
        BigDecimal puntajeTransformado,
        Long fkNivelRiesgo,
        String nivelRiesgo,
        List<ResultadoDetalleResponse> dominios,
        List<ResultadoDetalleResponse> dimensiones

) {
}
