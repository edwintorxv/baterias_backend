package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.Comparator;
import java.util.List;

/**
 * Frecuencia con que se respondió cada opción en los ítems de una dimensión de estrés.
 *
 * @param personas    personas que respondieron ítems de la dimensión
 * @param items       ítems (preguntas) de la dimensión que se respondieron
 * @param respuestas  total de respuestas de la dimensión, base de los porcentajes (personas × ítems
 *                    si nadie dejó ítems sin responder)
 * @param frecuencias una por opción, de la más frecuente (Siempre) a la menos (Nunca)
 */
public record FrecuenciaDimension(Long idDimensionCuestionario, String nombre, int personas, int items,
                                  int respuestas, List<ConteoFrecuencia> frecuencias) {

    /** Sin sumar opciones ({@link ConteoFrecuencia#compararMasFrecuentePrimero}). */
    public static Comparator<FrecuenciaDimension> masFrecuentePrimero() {
        return (a, b) -> ConteoFrecuencia.compararMasFrecuentePrimero(a.frecuencias(), b.frecuencias());
    }

}
