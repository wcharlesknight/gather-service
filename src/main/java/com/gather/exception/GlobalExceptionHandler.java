package com.gather.exception;

import com.gather.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Centralizes HTTP error mapping so every controller returns a consistent {@code {"error": ...}}
 * body. Replaces the per-controller try/catch blocks. Authentication/authorization failures
 * (401/403) are handled earlier by the security filter chain, not here.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidToken(InvalidTokenException e) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid or expired token.");
    }

    @ExceptionHandler(UnknownCityException.class)
    public ResponseEntity<Map<String, String>> handleUnknownCity(UnknownCityException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AuthService.EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailExists(AuthService.EmailAlreadyExistsException e) {
        return error(HttpStatus.CONFLICT, "This email is already registered.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "Validation failed.";
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception e) {
        logger.error("Unhandled exception", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.");
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
