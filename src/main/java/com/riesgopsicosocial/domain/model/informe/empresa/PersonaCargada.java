package com.riesgopsicosocial.domain.model.informe.empresa;

/**
 * Persona que la empresa entregó para evaluar (relación activa en {@code evaluado_cliente}).
 *
 * @param ciudad ciudad de trabajo; {@code null} si no está registrada
 */
public record PersonaCargada(Long idEvaluadoCliente, String area, CiudadTrabajo ciudad) {
}
