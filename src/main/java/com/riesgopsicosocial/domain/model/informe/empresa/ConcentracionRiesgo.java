package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * Concentración del riesgo por dominio intralaboral, ciudad de trabajo (regional) y área.
 * Intralaboral junta las formas A y B: cada persona cuenta con el nivel que le dio el baremo de su forma.
 *
 * @param dominios distribución por nivel de cada dominio intralaboral, del más crítico al menos crítico
 *                 ({@link DistribucionNiveles#masCriticaPrimero()}); empate: orden del cuestionario
 */
public record ConcentracionRiesgo(List<DominioConsolidado> dominios,
                                  DesgloseGrupos intralaboralPorCiudad, DesgloseGrupos intralaboralPorArea,
                                  DesgloseGrupos estresPorCiudad, DesgloseGrupos estresPorArea) {
}
