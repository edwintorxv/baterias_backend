package com.riesgopsicosocial.domain.model.resultado;

import java.util.Map;

/**
 * Etiquetas del manual de estrés (Tabla 6) para los niveles 1–5: la forma D usa los mismos
 * niveles de riesgo, pero los informes los muestran con estos nombres.
 */
public final class NivelEstres {

    private static final Map<Long, String> ETIQUETAS = Map.of(
            1L, "Muy bajo", 2L, "Bajo", 3L, "Medio", 4L, "Alto", 5L, "Muy alto");

    private NivelEstres() {
    }

    public static String etiqueta(Long idNivelRiesgo) {
        return ETIQUETAS.get(idNivelRiesgo);
    }

}
