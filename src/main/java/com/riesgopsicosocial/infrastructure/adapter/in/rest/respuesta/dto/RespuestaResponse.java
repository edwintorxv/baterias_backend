package com.riesgopsicosocial.infrastructure.adapter.in.rest.respuesta.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RespuestaResponse(

        Long id,
        Long fkAplicacion,
        Long fkPregunta,
        Long fkOpcionRespuesta,
        BigDecimal valorObtenido,
        LocalDateTime fechaRespuesta

) {
}
