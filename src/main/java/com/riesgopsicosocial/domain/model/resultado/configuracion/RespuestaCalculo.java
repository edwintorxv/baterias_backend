package com.riesgopsicosocial.domain.model.resultado.configuracion;

import java.math.BigDecimal;

public record RespuestaCalculo(
        Long idPregunta,
        Long idDimensionCuestionario,
        Long idCuestionario,
        BigDecimal valor
) {
}
