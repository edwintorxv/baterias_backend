package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * Factores de riesgo extralaborales (forma C) de la empresa.
 *
 * @param dimensiones de la más crítica a la menos crítica ({@link DistribucionNiveles#masCriticaPrimero()});
 *                    empate: orden del cuestionario
 * @param porCiudad   nivel total extralaboral por ciudad de trabajo, con los grupos pequeños protegidos
 * @param porArea     nivel total extralaboral por área, con los grupos pequeños protegidos
 */
public record ResultadosExtralaborales(List<DistribucionDimension> dimensiones, DesgloseGrupos porCiudad,
                                       DesgloseGrupos porArea) {
}
