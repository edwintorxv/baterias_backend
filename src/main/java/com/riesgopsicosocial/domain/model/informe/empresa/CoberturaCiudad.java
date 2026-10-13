package com.riesgopsicosocial.domain.model.informe.empresa;

/**
 * @param idCiudad {@code null} para las personas sin ciudad de trabajo registrada
 */
public record CoberturaCiudad(Long idCiudad, String ciudad, Cobertura cobertura) {
}
