package vn.edu.ptit.smarthms.common.error;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error response payload returned by the API for all error conditions.
 */
public record ErrorResponse(
        String code,
        String message,
        String correlationId,
        Instant timestamp,
        Map<String, Object> details
) {
    public static ErrorResponse of(String code, String message, String correlationId) {
        return new ErrorResponse(code, message, correlationId, Instant.now(), null);
    }

    public static ErrorResponse of(String code, String message, String correlationId, Map<String, Object> details) {
        return new ErrorResponse(code, message, correlationId, Instant.now(), details);
    }
}
