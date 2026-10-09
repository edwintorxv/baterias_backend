package com.riesgopsicosocial.domain.model.informe;

import java.time.LocalDateTime;

/**
 * Datos de una aplicación tal como estaban al aplicarla: cargo y área vienen de la
 * relación evaluado–cliente y el grupo ocupacional es la foto guardada en la aplicación.
 *
 * @param evaluador {@code null} si la aplicación no tiene evaluador asignado
 */
public record DatosAplicacion(
        Long idAplicacion,
        LocalDateTime fechaAplicacion,
        String nombreCargo,
        String nombreArea,
        Long idGrupoOcupacional,
        String grupoOcupacional,
        DatosEvaluador evaluador,
        String observaciones,
        String recomendaciones
) {
}
