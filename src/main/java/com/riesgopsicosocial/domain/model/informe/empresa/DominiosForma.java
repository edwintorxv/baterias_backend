package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * Distribución por dominio de una forma intralaboral (A o B), sobre las personas que la presentaron.
 *
 * @param dominios ranking, del más crítico al menos crítico ({@link DistribucionNiveles#masCriticaPrimero()});
 *                 en empate total, en el orden del cuestionario
 */
public record DominiosForma(Long idCuestionario, String forma, int total, List<DistribucionDominio> dominios) {
}
