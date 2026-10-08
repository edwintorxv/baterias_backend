package com.riesgopsicosocial.application.port.in.informe;

import com.riesgopsicosocial.domain.model.informe.InformeEvaluado;

import java.util.List;

public interface ConsultarInformeEvaluadoUseCase {

    /** @param anio año de las aplicaciones a incluir; {@code null} = historial completo */
    InformeEvaluado consultar(Long idCliente, Long idEvaluado, Integer anio);

    /** Años con aplicaciones del evaluado en el cliente, del más reciente al más antiguo. */
    List<Integer> consultarAnios(Long idCliente, Long idEvaluado);

}
