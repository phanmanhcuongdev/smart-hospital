package vn.edu.ptit.smarthms.common.error;

/**
 * Application-specific runtime exception that carries a typed ErrorCode.
 */
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public AppException(ErrorCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public AppException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail != null ? detail : errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public String getDetail() { return detail; }
}
