package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * @param idDominio   dominio común a las formas (A y B comparten los cuatro dominios): permite juntarlas
 * @param dimensiones dimensiones del dominio en el orden del cuestionario
 */
public record DominioInforme(Long idDominioCuestionario, Long idDominio, String nombre,
                             List<DimensionInforme> dimensiones) {
}
