package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * Frecuencia de síntomas del cuestionario de estrés, por dimensión y por ítem. El manual no trae
 * baremos por dimensión (solo para el total), así que no hay niveles: se muestra cuántas respuestas
 * fueron Siempre, Casi siempre, Algunas veces y Nunca.
 *
 * @param personas    personas con resultado de estrés, cuyas respuestas se cuentan
 * @param dimensiones de la de más síntomas frecuentes a la de menos ({@link FrecuenciaDimension#masFrecuentePrimero()});
 *                    empate: orden del cuestionario
 * @param porItem     cada dimensión con sus síntomas, en el orden del cuestionario (fisiológicos primero)
 */
public record SintomasEstres(int personas, List<FrecuenciaDimension> dimensiones, List<SintomasDimension> porItem) {
}
