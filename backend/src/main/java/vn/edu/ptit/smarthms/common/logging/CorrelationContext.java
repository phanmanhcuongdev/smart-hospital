package vn.edu.ptit.smarthms.common.logging;

import java.util.UUID;
import org.slf4j.MDC;

public final class CorrelationContext {

    private CorrelationContext() {}

    public static String getCorrelationId() {
        String corr = MDC.get(CorrelationLoggingFilter.CORRELATION_ID_MDC_KEY);
        return (corr != null && !corr.isBlank()) ? corr : UUID.randomUUID().toString();
    }

    public static void setCorrelationId(String correlationId) {
        if (correlationId != null) {
            MDC.put(CorrelationLoggingFilter.CORRELATION_ID_MDC_KEY, correlationId);
        }
    }

    public static void clear() {
        MDC.remove(CorrelationLoggingFilter.CORRELATION_ID_MDC_KEY);
    }
}
