package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * Nivel de riesgo intralaboral de la empresa por forma y la periodicidad de la evaluación que de
 * él resulta (Resolución 2764 de 2022, art. 3).
 *
 * @param formas       solo las formas que alguien presentó, en el orden del cuestionario
 * @param periodicidad {@code null} si no hay resultados intralaborales
 */
public record NivelRiesgoEmpresa(List<NivelForma> formas, Periodicidad periodicidad) {
}
