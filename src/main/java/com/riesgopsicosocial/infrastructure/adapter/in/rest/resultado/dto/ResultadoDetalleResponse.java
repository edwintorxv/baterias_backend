package com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto;

import java.math.BigDecimal;

/** Resultado de una dimensión o de un dominio (misma forma para ambos). */
public record ResultadoDetalleResponse(

        Long id,
        String nombre,
        BigDecimal puntajeBruto,
        BigDecimal puntajeTransformado,
        Long fkNivelRiesgo,
        String nivelRiesgo

) {
}
