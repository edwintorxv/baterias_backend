package com.riesgopsicosocial.domain.model.informe.empresa;

/**
 * Distribución de una dimensión intralaboral. Su total puede ser menor que el del dominio:
 * no cuenta a quienes la dimensión no les aplica (p. ej. relación con los colaboradores si
 * no son jefes).
 */
public record DistribucionDimension(Long idDimensionCuestionario, String nombre, DistribucionNiveles distribucion) {
}
