package com.riesgopsicosocial.domain.model.informe.empresa;

import java.math.BigDecimal;

/**
 * @param porcentaje cantidad / respuestas de la dimensión × 100, a un decimal
 */
public record ConteoFrecuencia(OpcionFrecuencia opcion, int cantidad, BigDecimal porcentaje) {
}
