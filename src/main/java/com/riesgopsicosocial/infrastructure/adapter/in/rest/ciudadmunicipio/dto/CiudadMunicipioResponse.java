package com.riesgopsicosocial.infrastructure.adapter.in.rest.ciudadmunicipio.dto;

public record CiudadMunicipioResponse(
        Long id,
        Long fkDepartamento,
        String codigoDane,
        String nombre
) {
}
