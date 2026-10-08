package com.riesgopsicosocial.domain.model.informe;

import java.util.List;

/**
 * Informe individual de un trabajador en un cliente: todas sus aplicaciones con ese
 * cliente, de la más antigua a la más reciente, para hacer seguimiento.
 */
public record InformeEvaluado(DatosCliente cliente, DatosEvaluado evaluado, List<AplicacionEvaluado> aplicaciones) {
}
