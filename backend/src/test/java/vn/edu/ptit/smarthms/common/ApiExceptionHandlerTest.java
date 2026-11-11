package vn.edu.ptit.smarthms.common;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.common.error.ApiExceptionHandler;
import vn.edu.ptit.smarthms.common.error.ForbiddenException;
import vn.edu.ptit.smarthms.common.error.NotFoundException;
import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.logging.CorrelationLoggingFilter;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @BeforeEach
    void setup() {
        MDC.put(CorrelationLoggingFilter.CORRELATION_ID_MDC_KEY, "test-mdc-correlation-id");
    }

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void handlesResponseStatusException404() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("NOT_FOUND", body.get("code"));
        assertEquals("Patient not found", body.get("message"));
        assertEquals("test-mdc-correlation-id", body.get("correlationId"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void handlesResponseStatusException422() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.valueOf(422), "Practitioner is inactive");
        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex);

        assertEquals(422, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("UNPROCESSABLE_ENTITY", body.get("code"));
        assertEquals("Practitioner is inactive", body.get("message"));
        assertEquals("test-mdc-correlation-id", body.get("correlationId"));
    }

    @Test
    void handlesNotFoundException() {
        NotFoundException ex = new NotFoundException("Employee not found");
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().get("code"));
        assertEquals("Employee not found", response.getBody().get("message"));
    }

    @Test
    void handlesForbiddenException() {
        ForbiddenException ex = new ForbiddenException("Actor lacks DOCTOR role");
        ResponseEntity<Map<String, Object>> response = handler.handleForbidden(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("FORBIDDEN", response.getBody().get("code"));
        assertEquals("Actor lacks DOCTOR role", response.getBody().get("message"));
    }

    @Test
    void handlesAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");
        ResponseEntity<Map<String, Object>> response = handler.handleForbidden(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("FORBIDDEN", response.getBody().get("code"));
    }

    @Test
    void handlesValidationException() {
        ValidationException ex = new ValidationException("Invalid date range");
        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().get("code"));
        assertEquals("Invalid date range", response.getBody().get("message"));
    }

    @Test
    void handlesHttpMessageNotReadableException() {
        org.springframework.mock.http.MockHttpInputMessage inputMessage =
                new org.springframework.mock.http.MockHttpInputMessage(new byte[0]);
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", inputMessage);
        ResponseEntity<Map<String, Object>> response = handler.handleNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MALFORMED_REQUEST", response.getBody().get("code"));
    }

    @Test
    void handlesUnexpectedException() {
        RuntimeException ex = new RuntimeException("Null pointer occurred");
        ResponseEntity<Map<String, Object>> response = handler.handleUnexpected(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().get("code"));
        assertEquals("An unexpected error occurred.", response.getBody().get("message"));
        assertEquals("test-mdc-correlation-id", response.getBody().get("correlationId"));
    }
}
