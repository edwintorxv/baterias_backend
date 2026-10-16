package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.time.LocalDateTime;

/**
 * @param guardado {@code false} si aún no se ha escrito nada para ese año
 */
public record ConclusionesInformeResponse(
        Long idCliente,
        int anio,
        boolean guardado,
        String conclusiones,
        String accionesCortoPlazo,
        String accionesMedianoPlazo,
        String accionesLargoPlazo,
        Long fkEvaluador,
        LocalDateTime fechaCreado,
        LocalDateTime fechaModificado
) {
}
