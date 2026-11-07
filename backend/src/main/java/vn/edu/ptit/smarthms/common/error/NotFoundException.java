package vn.edu.ptit.smarthms.common.error;

/** Resource requested by id was not found (maps to HTTP 404). */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
