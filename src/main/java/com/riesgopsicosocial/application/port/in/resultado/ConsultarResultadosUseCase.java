package com.riesgopsicosocial.application.port.in.resultado;

import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;

public interface ConsultarResultadosUseCase {

    ResultadoAplicacion consultar(Long idAplicacion);

}
