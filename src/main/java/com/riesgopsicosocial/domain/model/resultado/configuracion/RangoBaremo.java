package com.riesgopsicosocial.domain.model.resultado.configuracion;

import com.riesgopsicosocial.domain.model.resultado.NivelRiesgo;

import java.math.BigDecimal;

/**
 * Rango [valorMinimo, valorMaximo] de un baremo que corresponde a un nivel de riesgo.
 */
public record RangoBaremo(BigDecimal valorMinimo, BigDecimal valorMaximo, NivelRiesgo nivelRiesgo) {

    public boolean contiene(BigDecimal puntaje) {
        return puntaje.compareTo(valorMinimo) >= 0 && puntaje.compareTo(valorMaximo) <= 0;
    }

}
