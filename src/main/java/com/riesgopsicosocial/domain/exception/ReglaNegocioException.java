package com.riesgopsicosocial.domain.exception;

/**
 * Violación de una regla de negocio detectada en el dominio. El dominio no depende de
 * {@code shared}; el {@code GlobalExceptionHandler} la traduce a 409 igual que una
 * {@code BusinessException}.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String message) {
        super(message);
    }

}
