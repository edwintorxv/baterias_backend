package com.riesgopsicosocial.domain.model.informe.empresa;

import com.riesgopsicosocial.domain.model.resultado.configuracion.CondicionAplicacion;

/**
 * @param condicion pregunta filtro de la que depende; {@code null} si siempre aplica
 */
public record DimensionInforme(Long idDimensionCuestionario, String nombre, CondicionAplicacion condicion) {
}
