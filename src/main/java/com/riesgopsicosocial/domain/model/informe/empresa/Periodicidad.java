package com.riesgopsicosocial.domain.model.informe.empresa;

/**
 * Periodicidad de la evaluación de factores de riesgo psicosocial (Resolución 2764 de 2022, art. 3).
 */
public enum Periodicidad {

    /** Nivel intralaboral alto o muy alto en la forma A o en la B. */
    ANUAL("Anual"),
    /** Nivel intralaboral medio o bajo (o sin riesgo) en todas las formas presentadas. */
    CADA_DOS_ANIOS("Cada dos años");

    private final String descripcion;

    Periodicidad(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }

}
