package vn.edu.ptit.smarthms.common.audit;

/**
 * Utility to hold the current request's correlation ID in a ThreadLocal.
 */
public final class AuditContext {

    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    private AuditContext() {}

    public static String getCorrelationId() {
        return CORRELATION_ID.get();
    }

    public static void setCorrelationId(String correlationId) {
        CORRELATION_ID.set(correlationId);
    }

    public static void clear() {
        CORRELATION_ID.remove();
    }
}
