package vn.edu.ptit.smarthms.common;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import vn.edu.ptit.smarthms.common.logging.CorrelationLoggingFilter;

class CorrelationLoggingFilterTest {

    private final CorrelationLoggingFilter filter = new CorrelationLoggingFilter();

    @Test
    void generatesCorrelationIdWhenMissingInRequestHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/emr/encounters");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedMdcId = new String[1];
        FilterChain chain = (req, res) -> {
            capturedMdcId[0] = MDC.get(CorrelationLoggingFilter.CORRELATION_ID_MDC_KEY);
        };

        filter.doFilter(request, response, chain);

        String responseHeader = response.getHeader(CorrelationLoggingFilter.CORRELATION_ID_HEADER);
        assertNotNull(responseHeader);
        assertFalse(responseHeader.isBlank());
        assertEquals(responseHeader, capturedMdcId[0]);
        // Verified MDC is cleared after filter completes
        assertNull(MDC.get(CorrelationLoggingFilter.CORRELATION_ID_MDC_KEY));
    }

    @Test
    void preservesExistingCorrelationIdFromRequestHeader() throws ServletException, IOException {
        String existingId = "test-corr-id-12345";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/emr/encounters");
        request.addHeader(CorrelationLoggingFilter.CORRELATION_ID_HEADER, existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] capturedMdcId = new String[1];
        FilterChain chain = (req, res) -> {
            capturedMdcId[0] = MDC.get(CorrelationLoggingFilter.CORRELATION_ID_MDC_KEY);
        };

        filter.doFilter(request, response, chain);

        assertEquals(existingId, response.getHeader(CorrelationLoggingFilter.CORRELATION_ID_HEADER));
        assertEquals(existingId, capturedMdcId[0]);
        assertNull(MDC.get(CorrelationLoggingFilter.CORRELATION_ID_MDC_KEY));
    }
}
