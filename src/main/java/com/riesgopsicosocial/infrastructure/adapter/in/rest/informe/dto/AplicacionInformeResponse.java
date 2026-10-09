package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import com.riesgopsicosocial.infrastructure.adapter.in.rest.resultado.dto.ResultadoAplicacionResponse;

import java.time.LocalDateTime;

/**
 * @param edad             edad en el año de la aplicación (solo se conoce el año de nacimiento)
 * @param evaluador        {@code null} si la aplicación no tiene evaluador asignado
 * @param estadoResultados {@code CALCULADO} o {@code PENDIENTE} (en ese caso {@code resultados} es {@code null})
 */
public record AplicacionInformeResponse(

        Long fkAplicacion,
        LocalDateTime fechaAplicacion,
        String nombreCargo,
        String nombreArea,
        Integer edad,
        Long fkGrupoOcupacional,
        String grupoOcupacional,
        EvaluadorInformeResponse evaluador,
        String observaciones,
        String recomendaciones,
        String estadoResultados,
        ResultadoAplicacionResponse resultados

) {
}
