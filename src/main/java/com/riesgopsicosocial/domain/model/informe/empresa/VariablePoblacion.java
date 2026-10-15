package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * Una variable de la población evaluada (sexo, rango de edad…).
 *
 * @param categorias solo las que tienen personas, en el orden del catálogo; "Sin dato" al final
 */
public record VariablePoblacion(String nombre, List<ConteoCategoria> categorias) {
}
