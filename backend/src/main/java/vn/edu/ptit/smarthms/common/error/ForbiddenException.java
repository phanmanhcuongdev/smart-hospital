package vn.edu.ptit.smarthms.common.error;

/** Authenticated principal lacks the required role or scope (maps to HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
