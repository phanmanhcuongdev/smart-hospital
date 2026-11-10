package vn.edu.ptit.smarthms.common.error;

/** State or version conflict, e.g. stale row_version (maps to HTTP 409). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
