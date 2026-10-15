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

    /**
     * Orden sin sumar opciones: primero la de mayor porcentaje en "Siempre"; en empate, la de mayor
     * porcentaje en "Casi siempre", y así hacia abajo.
     */
    public static Comparator<FrecuenciaDimension> masFrecuentePrimero() {
        return (a, b) -> {
            for (int i = 0; i < a.frecuencias().size(); i++) {
                int comparacion = b.frecuencias().get(i).porcentaje().compareTo(a.frecuencias().get(i).porcentaje());
                if (comparacion != 0) {
                    return comparacion;
                }
            }
            return 0;
        };
    }

}
