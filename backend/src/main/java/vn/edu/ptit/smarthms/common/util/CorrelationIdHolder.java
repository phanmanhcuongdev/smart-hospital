package vn.edu.ptit.smarthms.common.util;

/**
 * Thread-local holder for the correlation ID associated with the current HTTP request.
 */
public final class CorrelationIdHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private CorrelationIdHolder() {}

    public static String get() {
        return HOLDER.get();
    }

    public static void set(String correlationId) {
        HOLDER.set(correlationId);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
