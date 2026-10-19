package com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluadocliente;

import com.riesgopsicosocial.infrastructure.adapter.out.persistence.evaluado.EvaluadoEntity;

/**
 * Resultado de vincular una persona a un cliente.
 *
 * @param datosPersonaActualizados la persona ya existía y llegaron nombre, apellido, sexo o año distintos
 *                                 (ganan los nuevos)
 */
public record Vinculacion(ResultadoVinculacion resultado, EvaluadoEntity evaluado, EvaluadoClienteEntity relacion,
                          boolean datosPersonaActualizados) {
}
