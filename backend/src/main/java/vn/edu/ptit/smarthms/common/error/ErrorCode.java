package vn.edu.ptit.smarthms.common.error;

/**
 * Enum of application error codes with associated HTTP status and default messages.
 */
public enum ErrorCode {
    AUTH_CREDENTIALS_INVALID(401, "AUTH_CREDENTIALS_INVALID", "Invalid username or password"),
    AUTH_TOKEN_EXPIRED(401, "AUTH_TOKEN_EXPIRED", "Session has expired"),
    AUTH_TOKEN_INVALID(401, "AUTH_TOKEN_INVALID", "Invalid or missing token"),
    AUTH_ACCOUNT_DISABLED(403, "AUTH_ACCOUNT_DISABLED", "Account is disabled"),
    ACCESS_DENIED(403, "ACCESS_DENIED", "Insufficient permissions"),
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "Resource not found"),
    CONFLICT(409, "CONFLICT", "Resource conflict"),
    VALIDATION_FAILED(422, "VALIDATION_FAILED", "Validation failed"),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "An unexpected error occurred");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}
