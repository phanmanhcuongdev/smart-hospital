package vn.edu.ptit.smarthms.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.ptit.smarthms.common.util.CorrelationIdHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private String correlationId() {
        String cid = CorrelationIdHolder.get();
        return (cid != null) ? cid : UUID.randomUUID().toString();
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        ErrorCode ec = ex.getErrorCode();
        String cid = correlationId();
        log.warn("[{}] AppException: code={} detail={}", cid, ec.getCode(), ex.getDetail());
        String message = ex.getDetail() != null ? ex.getDetail() : ec.getDefaultMessage();
        return ResponseEntity
                .status(ec.getHttpStatus())
                .body(ErrorResponse.of(ec.getCode(), message, cid));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String cid = correlationId();
        Map<String, Object> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        ErrorCode ec = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(ec.getCode(), ec.getDefaultMessage(), cid, fieldErrors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        String cid = correlationId();
        ErrorCode ec = ErrorCode.ACCESS_DENIED;
        log.warn("[{}] Access denied: {}", cid, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(ec.getCode(), ec.getDefaultMessage(), cid));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        String cid = correlationId();
        ErrorCode ec = ErrorCode.AUTH_TOKEN_INVALID;
        log.warn("[{}] Authentication exception: {}", cid, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ec.getCode(), ec.getDefaultMessage(), cid));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        String cid = correlationId();
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        log.error("[{}] Unexpected error", cid, ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ec.getCode(), ec.getDefaultMessage(), cid));
    }
}
