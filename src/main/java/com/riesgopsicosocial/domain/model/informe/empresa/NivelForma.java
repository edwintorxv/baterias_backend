package com.riesgopsicosocial.domain.model.informe.empresa;

import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;

import java.math.BigDecimal;

/**
 * Nivel de riesgo intralaboral de la empresa en una forma (Resolución 2764 de 2022, art. 3, parágrafo).
 *
 * @param personas             personas con resultado en la forma
 * @param promedioPuntajeBruto promedio del puntaje bruto total, a dos decimales
 * @param puntajeTransformado  promedio transformado con el factor de la forma, a un decimal
 */
public record NivelForma(Long idCuestionario, String forma, int personas, BigDecimal promedioPuntajeBruto,
                         BigDecimal puntajeTransformado, NivelRiesgo nivel) {
}
