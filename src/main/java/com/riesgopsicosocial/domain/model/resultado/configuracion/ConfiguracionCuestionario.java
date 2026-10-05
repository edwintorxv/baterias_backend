package com.riesgopsicosocial.domain.model.resultado.configuracion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estructura completa de un cuestionario (dimensiones, dominios, factores y baremos)
 * ya filtrada por el grupo ocupacional de la aplicación.
 */
public record ConfiguracionCuestionario(
        Long idCuestionario,
        String forma,
        MetodoCalculo metodoCalculo,
        BigDecimal factorTransformacion,
        Long idGrupoOcupacional,
        List<ConfiguracionDimension> dimensiones,
        List<ConfiguracionDominio> dominios,
        List<RangoBaremo> baremos
) {
}
