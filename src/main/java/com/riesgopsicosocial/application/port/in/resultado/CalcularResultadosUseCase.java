package com.riesgopsicosocial.application.port.in.resultado;

import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;

public interface CalcularResultadosUseCase {

    /**
     * Calcula (o recalcula) los resultados de todos los cuestionarios respondidos en la
     * aplicación. Reemplaza los resultados anteriores en la misma transacción.
     */
    ResultadoAplicacion calcular(Long idAplicacion);

}
