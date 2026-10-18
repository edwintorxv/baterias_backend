package com.riesgopsicosocial.shared.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.riesgopsicosocial.domain.exception.ReglaNegocioException;
import com.riesgopsicosocial.shared.response.ApiError;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiError error = ResponseBuilder.error(
                ex.getMessage(),
                ErrorCode.RESOURCE_NOT_FOUND.name(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler({BusinessException.class, ReglaNegocioException.class})
    public ResponseEntity<ApiError> handleBusinessException(RuntimeException ex) {
        ApiError error = ResponseBuilder.error(
                ex.getMessage(),
                ErrorCode.BUSINESS_ERROR.name(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(ValidationException ex) {
        ApiError error = ResponseBuilder.error(
                ex.getMessage(),
                ErrorCode.VALIDATION_ERROR.name(),
                ex.getDetails()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        ApiError error = ResponseBuilder.error(
                "Error de validación en los datos enviados",
                ErrorCode.VALIDATION_ERROR.name(),
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex) {
        ApiError error = ResponseBuilder.error(
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'",
                ErrorCode.VALIDATION_ERROR.name(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ApiError error = ResponseBuilder.error(
                "Valor no válido para el parámetro '" + ex.getName() + "': " + ex.getValue(),
                ErrorCode.VALIDATION_ERROR.name(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Cuerpo que no se puede leer: JSON mal formado o un valor que no es del tipo del campo (p. ej.
     * un rol que no existe). Para los enums dice los valores aceptados.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        String mensaje = "El cuerpo de la petición no es un JSON válido";
        List<String> details = null;
        if (ex.getCause() instanceof InvalidFormatException formato && !formato.getPath().isEmpty()) {
            String campo = formato.getPath().get(formato.getPath().size() - 1).getFieldName();
            mensaje = "Valor no válido para el campo '" + campo + "': " + formato.getValue();
            Class<?> tipo = formato.getTargetType();
            if (tipo != null && tipo.isEnum()) {
                details = List.of("Valores permitidos: " + Arrays.stream(tipo.getEnumConstants())
                        .map(Object::toString).collect(Collectors.joining(", ")));
            }
        }
        ApiError error = ResponseBuilder.error(mensaje, ErrorCode.VALIDATION_ERROR.name(), details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /** Inicio de sesión fallido (el mensaje no dice si falló el usuario o la contraseña). */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        ApiError error = ResponseBuilder.error(ex.getMessage(), ErrorCode.UNAUTHORIZED.name(), null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Permisos revisados en el controlador (firma propia, evaluador asignado): sin esto saldrían como
     * 500. Muestra el motivo propio; el "Access Denied" de Spring se deja con el mensaje general.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        String mensaje = ex.getMessage() == null || ex.getMessage().equals("Access Denied")
                ? "No tiene permiso para esta operación" : ex.getMessage();
        ApiError error = ResponseBuilder.error(mensaje, ErrorCode.FORBIDDEN.name(), null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        log.error("Error no controlado", ex);
        ApiError error = ResponseBuilder.error(
                "Ocurrió un error inesperado",
                ErrorCode.INTERNAL_SERVER_ERROR.name(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violacion de integirdad de datos {}", ex.getMostSpecificCause().getMessage());

        ApiError error = ResponseBuilder.error(
                "La operacion viola una restriccion de integridad de datos (verifique la referencia existe",
                ErrorCode.DATA_INTEGRITY_VIOLATION.name(),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}
