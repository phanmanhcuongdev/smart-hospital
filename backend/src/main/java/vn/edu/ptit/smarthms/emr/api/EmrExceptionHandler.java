package vn.edu.ptit.smarthms.emr.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/** Chỉ áp dụng cho controller EMR; ưu tiên hơn handler chung của common. */
@RestControllerAdvice(basePackageClasses = EncounterController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EmrExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalidBody(MethodArgumentNotValidException ex) {
        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .sorted().reduce((a, b) -> a + "; " + b).orElse("invalid request");
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, String>> malformed(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request or invalid identifier.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> status(ResponseStatusException ex) {
        HttpStatusCode code = ex.getStatusCode();
        String reason = ex.getReason() != null ? ex.getReason() : "Request failed.";
        return body(code, code.value() == 404 ? "NOT_FOUND" : "REQUEST_ERROR", reason);
    }

    private ResponseEntity<Map<String, String>> body(HttpStatusCode status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "code", code, "message", message,
                "correlationId", UUID.randomUUID().toString(),
                "timestamp", Instant.now().toString()));
    }
}