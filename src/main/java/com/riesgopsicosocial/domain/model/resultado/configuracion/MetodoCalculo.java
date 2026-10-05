package com.riesgopsicosocial.domain.model.resultado.configuracion;

/**
 * Forma en que se obtiene el puntaje de un cuestionario a partir de sus respuestas.
 */
public enum MetodoCalculo {

    /** Formas A y B: dimensión → dominio → total, sumando puntajes brutos. */
    SUMA_POR_DOMINIOS,

    /** Forma C (extralaboral): dimensión → total, sumando puntajes brutos. */
    SUMA_DIRECTA,

    /** Forma D (estrés): total = Σ promedio de cada dimensión × peso. */
    PROMEDIO_PONDERADO

}
