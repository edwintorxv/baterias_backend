package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * @param profesional profesional responsable; {@code null} si no se ha asignado
 * @param faltantes   lo que falta diligenciar para poder descargar el Word; vacío = se puede descargar
 */
public record ConclusionesEmpresaResponse(
        String conclusiones,
        String accionesCortoPlazo,
        String accionesMedianoPlazo,
        String accionesLargoPlazo,
        EvaluadorInformeResponse profesional,
        List<String> faltantes
) {
}
