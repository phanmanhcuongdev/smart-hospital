package vn.edu.ptit.smarthms.common.logging;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * Mô tả: Tiện ích lưu trữ và truy xuất Correlation ID trong MDC (Mapped Diagnostic Context)
 *        nhằm theo dõi vết luồng request xuyên suốt các dịch vụ dùng chung
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 11/11/2026
 */
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
