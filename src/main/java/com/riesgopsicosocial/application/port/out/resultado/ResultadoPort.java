package com.riesgopsicosocial.application.port.out.resultado;

import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;

public interface ResultadoPort {

    /** Borra los resultados existentes de la aplicación y guarda los nuevos. */
    void reemplazar(ResultadoAplicacion resultado);

    ResultadoAplicacion buscarPorAplicacion(Long idAplicacion, Long idGrupoOcupacional);

}
