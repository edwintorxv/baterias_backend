package com.riesgopsicosocial.domain.model.informe.empresa;

import com.riesgopsicosocial.domain.model.resultado.ResultadoAplicacion;
import com.riesgopsicosocial.domain.model.resultado.configuracion.FiltrosAplicacion;

/**
 * Persona que la empresa cargó para evaluar (relación activa en {@code evaluado_cliente}).
 *
 * @param area            área de trabajo tal como se cargó; {@code null} o vacía = sin área
 * @param ciudad          ciudad de trabajo; {@code null} si no está registrada
 * @param tieneRespuestas si respondió alguna prueba en el período (cuenta como evaluada)
 * @param resultado       resultados calculados en el período; {@code null} si no tiene
 * @param filtros         preguntas filtro de la aplicación de donde sale {@code resultado}: dicen qué
 *                        dimensiones condicionales no le aplican
 */
public record ParticipanteEmpresa(Long idEvaluadoCliente, String area, CiudadTrabajo ciudad, boolean tieneRespuestas,
                                  ResultadoAplicacion resultado, FiltrosAplicacion filtros) {

    public boolean pendienteDeCalculo() {
        return tieneRespuestas && resultado == null;
    }

}
