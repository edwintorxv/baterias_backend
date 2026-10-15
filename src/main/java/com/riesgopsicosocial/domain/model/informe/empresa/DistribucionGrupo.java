package com.riesgopsicosocial.domain.model.informe.empresa;

/**
 * Distribución por nivel de una prueba en un grupo (ciudad de trabajo o área).
 *
 * @param distribucion su {@code total} es la cantidad de personas del grupo con resultado de esa prueba
 */
public record DistribucionGrupo(String grupo, DistribucionNiveles distribucion) {
}
