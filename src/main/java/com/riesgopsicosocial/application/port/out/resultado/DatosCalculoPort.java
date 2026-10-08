package com.riesgopsicosocial.application.port.out.resultado;

import com.riesgopsicosocial.domain.model.resultado.configuracion.ConfiguracionCuestionario;
import com.riesgopsicosocial.domain.model.resultado.configuracion.FiltrosAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.RangoBaremo;
import com.riesgopsicosocial.domain.model.resultado.configuracion.RespuestaCalculo;

import java.util.List;
import java.util.Optional;

/**
 * Lectura de lo que el motor necesita: la aplicación, sus respuestas y la estructura
 * (dimensiones, dominios, factores y baremos) de cada cuestionario.
 */
public interface DatosCalculoPort {

    /** Grupo ocupacional guardado en la aplicación; vacío si la aplicación no existe. */
    Optional<Long> buscarGrupoOcupacional(Long idAplicacion);

    /** Preguntas filtro (atiende clientes, es jefe) guardadas en la aplicación. */
    FiltrosAplicacion buscarFiltros(Long idAplicacion);

    List<RespuestaCalculo> buscarRespuestas(Long idAplicacion);

    /** Estructura del cuestionario con los baremos del grupo ocupacional indicado. */
    ConfiguracionCuestionario cargarConfiguracion(Long idCuestionario, Long idGrupoOcupacional);

    /** Baremo del puntaje total general (intra + extra) para la forma intralaboral (A o B). */
    List<RangoBaremo> cargarBaremosTotalGeneral(Long idCuestionarioIntralaboral);

}
