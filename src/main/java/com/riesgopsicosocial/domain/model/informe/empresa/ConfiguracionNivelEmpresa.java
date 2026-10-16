package com.riesgopsicosocial.domain.model.informe.empresa;

import com.riesgopsicosocial.domain.model.resultado.configuracion.RangoBaremo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo necesario para el nivel de riesgo intralaboral de la empresa en una forma (A o B): su factor
 * de transformación y el baremo de su puntaje total.
 */
public record ConfiguracionNivelEmpresa(Long idCuestionario, String forma, BigDecimal factorTransformacion,
                                        List<RangoBaremo> baremos) {
}
