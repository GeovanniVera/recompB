package com.geovanni.miscalorias.shared.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus();
        ApiError body = new ApiError(Instant.now(), status.value(), ex.getCode(), ex.getMessage(),
                request.getRequestURI(), null);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (ex instanceof OtpCooldownException cooldown) {
            builder.header("Retry-After", String.valueOf(Math.max(1, cooldown.getRetryAfter().toSeconds())));
        }
        return builder.body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "validation_failed",
                "Los datos enviados no son válidos.", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        // Jackson wraps invalid enum values and malformed JSON here. Without this handler
        // the generic Exception handler would answer 500 for a client-side mistake.
        String message = "No pudimos leer los datos enviados.";
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof IllegalArgumentException && cause.getMessage() != null) {
            message = cause.getMessage();
        }
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "malformed_body",
                message, request.getRequestURI(), null);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "invalid_parameter",
                "El parámetro '%s' tiene un valor inválido.".formatted(ex.getName()),
                request.getRequestURI(), null);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "missing_parameter",
                "Falta el parámetro '%s'.".formatted(ex.getParameterName()),
                request.getRequestURI(), null);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ApiError body = new ApiError(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "internal_error", "Ocurrió un error inesperado.", request.getRequestURI(), null);
        return ResponseEntity.internalServerError().body(body);
    }
}
