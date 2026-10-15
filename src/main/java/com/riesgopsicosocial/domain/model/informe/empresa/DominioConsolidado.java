package com.riesgopsicosocial.domain.model.informe.empresa;

/**
 * Un dominio intralaboral con las formas A y B juntas.
 */
public record DominioConsolidado(Long idDominio, String nombre, DistribucionNiveles distribucion) {
}
