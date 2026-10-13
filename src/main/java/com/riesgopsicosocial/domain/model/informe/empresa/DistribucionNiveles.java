package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.Comparator;
import java.util.List;

/**
 * Cantidad y porcentaje de personas en cada nivel (siempre los cinco, aunque estén en 0).
 *
 * @param total   personas con resultado en lo que se distribuye
 * @param niveles de menor a mayor riesgo
 */
public record DistribucionNiveles(int total, List<ConteoNivel> niveles) {

    /**
     * Orden de los rankings, sin sumar niveles: primero la de mayor porcentaje en el nivel más
     * alto (muy alto); en empate, la de mayor porcentaje en el siguiente (alto), y así hacia abajo.
     */
    public static Comparator<DistribucionNiveles> masCriticaPrimero() {
        return (a, b) -> {
            for (int i = a.niveles().size() - 1; i >= 0; i--) {
                int comparacion = b.niveles().get(i).porcentaje().compareTo(a.niveles().get(i).porcentaje());
                if (comparacion != 0) {
                    return comparacion;
                }
            }
            return 0;
        };
    }

}
