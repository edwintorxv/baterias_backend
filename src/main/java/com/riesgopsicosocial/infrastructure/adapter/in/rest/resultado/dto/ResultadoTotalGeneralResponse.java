package com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto;

import java.math.BigDecimal;

public record ResultadoTotalGeneralResponse(

        Long fkCuestionarioIntralaboral,
        String formaIntralaboral,
        BigDecimal puntajeBruto,
        BigDecimal puntajeTransformado,
        Long fkNivelRiesgo,
        String nivelRiesgo

) {
}
