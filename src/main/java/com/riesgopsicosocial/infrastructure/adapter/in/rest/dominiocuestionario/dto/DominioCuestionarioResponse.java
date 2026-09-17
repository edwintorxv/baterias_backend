package com.riesgopsicosocial.infrastructure.adapter.in.rest.dominiocuestionario.dto;

import java.math.BigDecimal;

public record DominioCuestionarioResponse(
        Long id,
        Long fkDominio,
        Long fkCuestionario,
        BigDecimal factorTransformacion) {
}
