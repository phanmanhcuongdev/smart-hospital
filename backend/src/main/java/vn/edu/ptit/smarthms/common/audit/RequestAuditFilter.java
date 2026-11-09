package vn.edu.ptit.smarthms.common.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.ptit.smarthms.common.util.CorrelationIdHolder;

import java.io.IOException;
import java.util.UUID;

/**
 * Generates a correlationId per request, stores it in MDC and CorrelationIdHolder,
 * and adds the X-Correlation-Id response header.
 */
@Component
public class RequestAuditFilter extends OncePerRequestFilter {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String incomingCid = request.getHeader(CORRELATION_HEADER);
        String correlationId = (incomingCid != null && !incomingCid.isBlank())
                ? incomingCid.trim()
                : UUID.randomUUID().toString();
        MDC.put(MDC_KEY, correlationId);
        CorrelationIdHolder.set(correlationId);
        AuditContext.setCorrelationId(correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
            CorrelationIdHolder.clear();
            AuditContext.clear();
        }
    }
}
