package com.riesgopsicosocial.domain.model.informe.empresa;

import com.riesgopsicosocial.domain.model.informe.DatosCliente;

/**
 * Informe de una empresa en un año.
 *
 * @param general       análisis de todas las personas cargadas por la empresa
 * @param concentracion concentración del riesgo por dominio intralaboral, ciudad y área (intralaboral y estrés)
 * @param sintomas      frecuencia de síntomas por dimensión del cuestionario de estrés
 */
public record InformeEmpresa(DatosCliente cliente, int anio, AnalisisGrupo general,
                             ConcentracionRiesgo concentracion, SintomasEstres sintomas) {
}
