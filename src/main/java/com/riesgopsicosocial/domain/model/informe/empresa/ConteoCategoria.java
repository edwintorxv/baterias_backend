package com.riesgopsicosocial.domain.model.informe.empresa;

import java.math.BigDecimal;

/**
 * @param porcentaje cantidad / personas evaluadas × 100, a un decimal
 */
public record ConteoCategoria(String categoria, int cantidad, BigDecimal porcentaje) {
}
