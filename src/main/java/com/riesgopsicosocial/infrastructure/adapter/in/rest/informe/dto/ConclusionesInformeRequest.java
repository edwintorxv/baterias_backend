package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

/**
 * Reemplaza todo lo guardado del año: un campo que no venga (o en blanco) queda vacío.
 *
 * @param fkEvaluador profesional responsable (debe estar activo al asignarlo); {@code null} = sin asignar
 */
public record ConclusionesInformeRequest(
        String conclusiones,
        String accionesCortoPlazo,
        String accionesMedianoPlazo,
        String accionesLargoPlazo,
        Long fkEvaluador
) {
}
