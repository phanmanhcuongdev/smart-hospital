package vn.edu.ptit.smarthms.common.error;

import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import vn.edu.ptit.smarthms.common.logging.CorrelationContext;

/**
 * Global API exception handler for both EMR and HR modules,
 * formatting all error responses into a unified JSON structure:
 * { "code": string, "message": string, "correlationId": string, "timestamp": string }
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String code = switch (status.value()) {
            case 400 -> "VALIDATION_ERROR";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 422 -> "UNPROCESSABLE_ENTITY";
            default -> "REQUEST_ERROR";
        };
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        log.warn("Handled ResponseStatusException [status={}, code={}]: {}", status.value(), code, reason);
        return error(HttpStatus.valueOf(status.value()), code, reason);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted().reduce((a, b) -> a + "; " + b).orElse("Validation failed.");
        log.warn("Handled MethodArgumentNotValidException: {}", detail);
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Handled HttpMessageNotReadableException: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed JSON request body or invalid format.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value for parameter: " + ex.getName() + ".";
        log.warn("Handled MethodArgumentTypeMismatchException: {}", message);
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException ex) {
        log.warn("Handled ValidationException: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        log.warn("Handled NotFoundException: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Handled UnauthorizedException [code={}]: {}", ex.getCode(), ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleForbidden(Exception ex) {
        log.warn("Handled AccessDenied / ForbiddenException: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN",
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "You do not have access to this resource.");
    }

    @ExceptionHandler({ConflictException.class, OptimisticLockException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex) {
        String message = ex instanceof ConflictException ? ex.getMessage()
                : "The record was changed by someone else. Reload and try again.";
        log.warn("Handled ConflictException: {}", message);
        return error(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        String correlationId = CorrelationContext.getCorrelationId();
        log.error("Unhandled exception [correlationId={}]: {}", correlationId, ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("correlationId", CorrelationContext.getCorrelationId());
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
