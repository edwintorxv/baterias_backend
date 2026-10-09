package com.riesgopsicosocial.domain.model.informe;

/**
 * @param tipoContenido {@code image/png} o {@code image/jpeg}
 */
public record FirmaEvaluador(byte[] imagen, String tipoContenido) {
}
