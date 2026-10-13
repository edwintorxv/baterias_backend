package com.riesgopsicosocial.domain.model.informe;

import java.util.Arrays;
import java.util.Optional;

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

    /** Formato por su extensión, sin distinguir mayúsculas ni espacios. */
    public static Optional<FormatoDocumento> deExtension(String extension) {
        return Arrays.stream(values())
                .filter(f -> f.extension.equalsIgnoreCase(extension.trim()))
                .findFirst();
    }

}
