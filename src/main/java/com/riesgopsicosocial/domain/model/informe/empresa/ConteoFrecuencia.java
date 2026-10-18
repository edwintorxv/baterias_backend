package com.riesgopsicosocial.domain.model.informe.empresa;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param porcentaje cantidad / respuestas de la dimensión (o del ítem) × 100, a un decimal
 */
public record ConteoFrecuencia(OpcionFrecuencia opcion, int cantidad, BigDecimal porcentaje) {

    /**
     * Orden sin sumar opciones: primero la de mayor porcentaje en "Siempre"; en empate, la de mayor
     * porcentaje en "Casi siempre", y así hacia abajo. Negativo si {@code a} va antes que {@code b}.
     */
    public static int compararMasFrecuentePrimero(List<ConteoFrecuencia> a, List<ConteoFrecuencia> b) {
        for (int i = 0; i < a.size(); i++) {
            int comparacion = b.get(i).porcentaje().compareTo(a.get(i).porcentaje());
            if (comparacion != 0) {
                return comparacion;
            }
        }
        return 0;
    }

}
