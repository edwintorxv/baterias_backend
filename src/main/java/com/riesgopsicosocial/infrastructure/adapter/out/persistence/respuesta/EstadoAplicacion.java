package com.riesgopsicosocial.infrastructure.adapter.out.persistence.respuesta;

/**
 * En qué va una aplicación, calculado (no se guarda): reemplaza en la pantalla al texto libre
 * {@code aplicacion.estado}.
 */
public enum EstadoAplicacion {

    /** Creada, sin ninguna respuesta. */
    SIN_RESPUESTAS,

    /** Le faltan respuestas a alguna pregunta que aplica. */
    INCOMPLETA,

    /** Todas las respuestas, sin resultados (o se borraron al corregir). */
    PENDIENTE_CALCULO,

    /** Con resultados calculados. */
    CALCULADA;

    /** La misma regla para el formulario y el listado: sin respuestas primero, luego con resultados. */
    public static EstadoAplicacion de(boolean conRespuestas, boolean conResultados, boolean completa) {
        if (!conRespuestas) {
            return SIN_RESPUESTAS;
        }
        if (conResultados) {
            return CALCULADA;
        }
        return completa ? PENDIENTE_CALCULO : INCOMPLETA;
    }

}
