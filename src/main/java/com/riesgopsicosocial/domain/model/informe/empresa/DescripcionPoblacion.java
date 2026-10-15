package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * Descripción de las personas evaluadas (las que respondieron alguna prueba en el año).
 *
 * @param personas  personas evaluadas: el total de cada variable
 * @param variables forma aplicada, sexo, rango de edad, nivel académico, tipo de cargo y tipo de contrato
 */
public record DescripcionPoblacion(int personas, List<VariablePoblacion> variables) {
}
