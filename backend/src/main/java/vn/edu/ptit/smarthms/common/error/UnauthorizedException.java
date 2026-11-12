package vn.edu.ptit.smarthms.common.error;

/** Caller is not authenticated or sent wrong credentials (maps to HTTP 401). */
public class UnauthorizedException extends RuntimeException {

    private final String code;

    public UnauthorizedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
