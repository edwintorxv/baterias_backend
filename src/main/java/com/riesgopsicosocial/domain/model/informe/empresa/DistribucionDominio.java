package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * @param dimensiones ranking de sus dimensiones, de la más crítica a la menos crítica
 *                    ({@link DistribucionNiveles#masCriticaPrimero()})
 */
public record DistribucionDominio(Long idDominioCuestionario, String nombre, DistribucionNiveles distribucion,
                                  List<DistribucionDimension> dimensiones) {
}
