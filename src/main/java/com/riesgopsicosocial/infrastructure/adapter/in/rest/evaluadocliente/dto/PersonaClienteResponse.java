package com.riesgopsicosocial.infrastructure.adapter.in.rest.evaluadocliente.dto;

import java.time.LocalDateTime;

/**
 * Persona del cliente para el listado del front: datos del evaluado, su relación con el cliente y la
 * última aplicación ({@code null} si no tiene ninguna).
 */
public record PersonaClienteResponse(
        Long idRelacion,
        Long idEvaluado,
        String numeroIdentificacion,
        String nombre,
        String apellido,
        String nombreCargo,
        String tipoCargo,
        String nombreArea,
        String ciudadTrabajo,
        Boolean activo,
        long aplicaciones,
        UltimaAplicacion ultimaAplicacion
) {

    /**
     * @param conRespuestas la persona ya respondió (sin respuestas, la aplicación solo está creada)
     * @param conResultados ya se calcularon sus resultados (si no, el informe individual aún no sale)
     */
    public record UltimaAplicacion(Long id, LocalDateTime fecha, boolean conRespuestas,
                                   boolean conResultados) {
    }

}
