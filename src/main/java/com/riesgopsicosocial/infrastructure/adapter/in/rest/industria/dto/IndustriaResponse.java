package com.riesgopsicosocial.infrastructure.adapter.in.rest.industria.dto;

/** Industria con su sector económico, para el select en cascada sector → industria. */
public record IndustriaResponse(
        Long id,
        Long fkSectorEconomico,
        String nombre
) {
}
