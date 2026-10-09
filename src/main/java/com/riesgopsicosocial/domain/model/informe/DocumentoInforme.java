package com.riesgopsicosocial.domain.model.informe;

public record DocumentoInforme(String nombreArchivo, FormatoDocumento formato, byte[] contenido) {
}
