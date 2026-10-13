package com.riesgopsicosocial.domain.model.informe.empresa;

/**
 * Ciudad donde trabaja la persona. Se agrupa por id: hay municipios con el mismo nombre
 * en distintos departamentos.
 */
public record CiudadTrabajo(Long id, String nombre) {
}
