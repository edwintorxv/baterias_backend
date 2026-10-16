package com.riesgopsicosocial.domain.model.informe.empresa;

import com.riesgopsicosocial.domain.model.informe.DatosCliente;

/**
 * Informe de una empresa en un año.
 *
 * @param general       análisis de todas las personas cargadas por la empresa
 * @param poblacion     descripción de las personas evaluadas (forma, sexo, edad, nivel académico, cargo y contrato)
 * @param concentracion concentración del riesgo por dominio intralaboral, ciudad y área (intralaboral y estrés)
 * @param extralaboral  dimensiones extralaborales y nivel total extralaboral por ciudad y área
 * @param sintomas      frecuencia de síntomas por dimensión del cuestionario de estrés
 * @param conclusiones  conclusiones, plan de acción y profesional responsable ({@link ConclusionesInforme#VACIAS} si no hay)
 */
public record InformeEmpresa(DatosCliente cliente, int anio, AnalisisGrupo general, DescripcionPoblacion poblacion,
                             ConcentracionRiesgo concentracion, ResultadosExtralaborales extralaboral,
                             SintomasEstres sintomas, ConclusionesInforme conclusiones) {
}
