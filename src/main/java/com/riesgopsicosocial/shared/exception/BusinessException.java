package com.riesgopsicosocial.shared.exception;

import java.util.List;

public class BusinessException extends RuntimeException {

    private final List<String> details;

    public BusinessException(String message) {
        this(message, null);
    }

    /** Con detalles (p. ej. las filas con error de un archivo), que salen en {@code ApiError.details}. */
    public BusinessException(String message, List<String> details) {
        super(message);
        this.details = details;
    }

    public List<String> getDetails() {
        return details;
    }

}
