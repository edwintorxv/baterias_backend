package com.riesgopsicosocial.domain.model.resultado.configuracion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dimensión de un cuestionario tal como la necesita el cálculo.
 *
 * @param idDominioCuestionario dominio al que pertenece dentro del mismo cuestionario (solo SUMA_POR_DOMINIOS)
 * @param peso                  solo PROMEDIO_PONDERADO
 * @param condicion             pregunta filtro de la que depende; {@code null} si siempre aplica
 * @param maxItemsSinRespuesta  ítems que pueden faltar sin invalidar la dimensión
 * @param baremos               baremos del grupo ocupacional de la aplicación
 */
public record ConfiguracionDimension(
        Long idDimensionCuestionario,
        Long idDominioCuestionario,
        String nombre,
        BigDecimal factorTransformacion,
        BigDecimal peso,
        int numeroPreguntas,
        CondicionAplicacion condicion,
        int maxItemsSinRespuesta,
        List<RangoBaremo> baremos
) {
}
