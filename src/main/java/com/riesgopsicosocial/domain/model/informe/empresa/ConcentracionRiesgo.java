package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * Concentración del riesgo por dominio intralaboral, ciudad de trabajo (regional) y área.
 * Intralaboral junta las formas A y B: cada persona cuenta con el nivel que le dio el baremo de su forma.
 *
 * @param dominios  distribución por nivel de cada dominio intralaboral, del más crítico al menos crítico
 *                  ({@link DistribucionNiveles#masCriticaPrimero()}); empate: orden del cuestionario
 * @param porCiudad intralaboral total y estrés por ciudad de trabajo, de la de más personas a la de menos
 * @param porArea   intralaboral total y estrés por área, de la de más personas a la de menos
 */
public record ConcentracionRiesgo(List<DominioConsolidado> dominios, List<DistribucionGrupo> porCiudad,
                                  List<DistribucionGrupo> porArea) {
}
