package com.riesgopsicosocial.domain.model.informe.empresa;

import com.riesgopsicosocial.domain.model.informe.DatosCliente;

/**
 * Informe de una empresa en un año.
 *
 * @param general análisis de todas las personas cargadas por la empresa
 */
public record InformeEmpresa(DatosCliente cliente, int anio, AnalisisGrupo general) {
}
