package com.riesgopsicosocial.domain.model.resultado.configuracion;

import java.math.BigDecimal;
import java.util.List;

public record ConfiguracionDominio(
        Long idDominioCuestionario,
        String nombre,
        BigDecimal factorTransformacion,
        List<RangoBaremo> baremos
) {
}
