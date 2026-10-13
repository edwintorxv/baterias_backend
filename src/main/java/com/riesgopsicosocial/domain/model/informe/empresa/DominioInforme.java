package com.riesgopsicosocial.domain.model.informe.empresa;

import java.util.List;

/**
 * @param dimensiones dimensiones del dominio en el orden del cuestionario
 */
public record DominioInforme(Long idDominioCuestionario, String nombre, List<DimensionInforme> dimensiones) {
}
