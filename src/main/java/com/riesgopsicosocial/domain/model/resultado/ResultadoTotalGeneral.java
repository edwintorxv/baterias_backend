package com.riesgopsicosocial.domain.model.resultado;

import java.math.BigDecimal;

/**
 * Puntaje total general de factores de riesgo psicosocial: intralaboral (forma A o B)
 * más extralaboral. Se interpreta con el baremo de la forma intralaboral.
 */
public record ResultadoTotalGeneral(
        Long idCuestionarioIntralaboral,
        String formaIntralaboral,
        BigDecimal puntajeBruto,
        BigDecimal puntajeTransformado,
        NivelRiesgo nivelRiesgo
) {
}
