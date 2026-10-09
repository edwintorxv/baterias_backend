package com.riesgopsicosocial.domain.model.informe;

public enum FormatoDocumento {

    PDF("pdf"),
    DOCX("docx");

    private final String extension;

    FormatoDocumento(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

}
