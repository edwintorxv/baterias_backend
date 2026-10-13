package com.riesgopsicosocial.domain.model.informe.empresa;

import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.FiltrosAplicacion;

import java.time.LocalDateTime;

/**
 * Aplicación de una persona de la empresa en el período del informe.
 *
 * @param resultado {@code null} si aún no tiene resultados calculados
 * @param filtros   respuestas a las preguntas filtro (atiende clientes, es jefe)
 */
public record AplicacionEmpresa(Long idAplicacion, Long idEvaluadoCliente, LocalDateTime fechaAplicacion,
                                boolean tieneRespuestas, ResultadoAplicacion resultado, FiltrosAplicacion filtros) {
}
