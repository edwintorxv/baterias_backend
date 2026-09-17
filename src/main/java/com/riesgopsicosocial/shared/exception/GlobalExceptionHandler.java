package com.riesgopsicosocial.shared.exception;

import com.riesgopsicosocial.shared.response.ApiError;
import com.riesgopsicosocial.shared.response.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex) {
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
