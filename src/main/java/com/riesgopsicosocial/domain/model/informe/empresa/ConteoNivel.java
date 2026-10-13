package com.riesgopsicosocial.domain.model.informe.empresa;

import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;

import java.math.BigDecimal;

/**
 * @param porcentaje cantidad / total de la distribución × 100, a un decimal
 */
public record ConteoNivel(NivelRiesgo nivel, int cantidad, BigDecimal porcentaje) {
}
