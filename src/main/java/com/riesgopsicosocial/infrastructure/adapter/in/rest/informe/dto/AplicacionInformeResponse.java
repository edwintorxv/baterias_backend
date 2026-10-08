package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto.ResultadoAplicacionResponse;

import java.time.LocalDateTime;

/**
 * @param estadoResultados {@code CALCULADO} o {@code PENDIENTE} (en ese caso {@code resultados} es {@code null})
 */
public record AplicacionInformeResponse(

        Long fkAplicacion,
        LocalDateTime fechaAplicacion,
        String nombreCargo,
        String nombreArea,
        Long fkGrupoOcupacional,
        String grupoOcupacional,
        String estadoResultados,
        ResultadoAplicacionResponse resultados

) {
}
