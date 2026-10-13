package com.riesgopsicosocial.domain.model.informe.empresa;

import java.math.BigDecimal;

/**
 * @param esperados  personas cargadas por la empresa para evaluar
 * @param evaluados  de ellas, las que respondieron alguna prueba
 * @param porcentaje evaluados / esperados × 100, a un decimal
 */
public record Cobertura(int esperados, int evaluados, BigDecimal porcentaje) {
}
