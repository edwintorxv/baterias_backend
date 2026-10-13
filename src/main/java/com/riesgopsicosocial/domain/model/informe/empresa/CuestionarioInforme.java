package com.riesgopsicosocial.domain.model.informe.empresa;

import com.riesgopsicosocial.domain.model.resultado.configuracion.MetodoCalculo;

import java.util.List;

/**
 * Estructura de un cuestionario para el informe de empresa: define en qué tabla cuenta
 * (por su método de cálculo) y el orden de sus dominios.
 */
public record CuestionarioInforme(Long idCuestionario, String forma, MetodoCalculo metodoCalculo,
                                  List<DominioInforme> dominios) {
}
