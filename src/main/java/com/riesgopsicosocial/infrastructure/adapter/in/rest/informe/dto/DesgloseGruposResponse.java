package com.riesgopsicosocial.infrastructure.adapter.in.rest.informe.dto;

import java.util.List;

/**
 * @param grupos    de más a menos personas; "Otras ciudades" / "Otras áreas", si existe, al final
 * @param agrupados grupos pequeños que se juntaron en "Otras…"
 * @param omitidos  personas que no se muestran porque sus grupos, aun juntos, no llegan al mínimo
 */
public record DesgloseGruposResponse(
        List<DistribucionGrupoResponse> grupos,
        int agrupados,
        int omitidos
) {
}
