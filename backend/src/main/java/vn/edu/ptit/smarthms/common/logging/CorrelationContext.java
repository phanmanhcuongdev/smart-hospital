package vn.edu.ptit.smarthms.common.logging;

import java.util.UUID;
import org.slf4j.MDC;

public final class CorrelationContext {

    private CorrelationContext() {}

    public static String getCorrelationId() {
        String corr = MDC.get(CorrelationLoggingFilter.CORRELATION_ID_MDC_KEY);
        return (corr != null && !corr.isBlank()) ? corr : UUID.randomUUID().toString();
    }
}
