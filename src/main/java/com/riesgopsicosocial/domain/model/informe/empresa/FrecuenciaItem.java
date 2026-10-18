package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.Comparator;
import java.util.List;

/**
 * Frecuencia con que se respondió cada opción en un ítem (síntoma) del cuestionario de estrés.
 *
 * @param respuestas  personas que respondieron el ítem, base de los porcentajes
 * @param frecuencias una por opción, de la más frecuente (Siempre) a la menos (Nunca)
 */
public record FrecuenciaItem(Long idPregunta, int numero, String descripcion, int respuestas,
                             List<ConteoFrecuencia> frecuencias) {

    /** Sin sumar opciones ({@link ConteoFrecuencia#compararMasFrecuentePrimero}). */
    public static Comparator<FrecuenciaItem> masFrecuentePrimero() {
        return (a, b) -> ConteoFrecuencia.compararMasFrecuentePrimero(a.frecuencias(), b.frecuencias());
    }

}
