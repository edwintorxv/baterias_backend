package com.riesgopsicosocial.infrastructure.adapter.in.rest.tipocargo.dto;

/** Tipo de cargo con su grupo ocupacional (1 → forma A, 2 → forma B), para avisar si la forma elegida no coincide. */
public record TipoCargoResponse(
        Long id,
        Long fkGrupoOcupacional,
        String nombre
) {
}
