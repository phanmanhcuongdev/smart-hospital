package vn.edu.ptit.smarthms.common.error;

/** Business validation failed (maps to HTTP 400). */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
