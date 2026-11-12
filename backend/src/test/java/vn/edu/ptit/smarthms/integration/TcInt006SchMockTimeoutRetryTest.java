package vn.edu.ptit.smarthms.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.ptit.smarthms.common.logging.CorrelationContext;
import vn.edu.ptit.smarthms.hr.domain.IntegrationOutbox;
import vn.edu.ptit.smarthms.hr.domain.IntegrationOutboxRepository;
import vn.edu.ptit.smarthms.hr.service.SchIntegrationDispatcher;
import vn.edu.ptit.smarthms.integration.mock.MockSchedulerClient;

/**
 * TC-INT-006: SCH mock timeout + retry
 */
class TcInt006SchMockTimeoutRetryTest {

    private final String correlationId = "CORR-TC-INT-006-" + UUID.randomUUID();
    private SchIntegrationDispatcher dispatcher;
    private MockSchedulerClient schMock;
    private Map<UUID, IntegrationOutbox> outboxDatabase;
    private IntegrationOutbox event;

    @BeforeEach
    void setUp() {
        CorrelationContext.setCorrelationId(correlationId);
        outboxDatabase = new HashMap<>();
        schMock = new MockSchedulerClient();

        UUID employeeId = UUID.randomUUID();
        event = new IntegrationOutbox("EMPLOYEE_TERMINATED:" + employeeId + ":" + UUID.randomUUID(),
                employeeId, "{\"decisionNumber\":\"DEC-TEST-001\"}");
        outboxDatabase.put(UUID.randomUUID(), event);

        IntegrationOutboxRepository outboxRepo = (IntegrationOutboxRepository) Proxy.newProxyInstance(
                IntegrationOutboxRepository.class.getClassLoader(),
                new Class<?>[]{IntegrationOutboxRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("save")) {
                        return args[0];
                    }
                    if (name.equals("findByDeliveryState")) {
                        IntegrationOutbox.DeliveryState s = (IntegrationOutbox.DeliveryState) args[0];
                        return outboxDatabase.values().stream().filter(o -> o.getDeliveryState() == s).toList();
                    }
                    return null;
                });

        dispatcher = new SchIntegrationDispatcher(outboxRepo);
    }

    @Test
    @DisplayName("TC-INT-006: SCH mock timeout -> Giữ PENDING, tăng attempt (không success giả), sau đó hồi phục mock replay thành công")
    void testSchMockTimeoutAndRetry() {
        // Step 1: Cho SCH mock trả timeout sau khi HR commit
        schMock.setSimulateTimeout(true);

        // Lần dispatch 1: Bị Timeout
        SchIntegrationDispatcher.DispatchResult result1 = dispatcher.dispatchPendingEvents(schMock);
        assertEquals(0, result1.successCount());
        assertEquals(1, result1.retryPendingCount(), "Event rơi vào danh sách retry pending");

        // Step 2: Kiểm state pending và event retry
        assertEquals(IntegrationOutbox.DeliveryState.PENDING, event.getDeliveryState(),
                "Trạng thái outbox vẫn giữ PENDING, không báo success giả");
        assertEquals(1, event.getAttempts(), "Số lần thử tăng lên 1");
        assertNotNull(event.getLastAttemptAt());
        assertTrue(schMock.getDeliveredKeys().isEmpty(), "Mock chưa nhận được event do timeout");

        // Step 3: Mở lại mock (hết timeout), replay eventId cũ
        schMock.setSimulateTimeout(false);

        SchIntegrationDispatcher.DispatchResult result2 = dispatcher.dispatchPendingEvents(schMock);
        assertEquals(1, result2.successCount(), "Replay thành công");
        assertEquals(IntegrationOutbox.DeliveryState.SENT, event.getDeliveryState());
        assertEquals(2, event.getAttempts(), "Tổng số lần thử là 2");

        // Downstream không duplicate: mock chỉ ghi nhận duy nhất 1 lần nhận thành công
        assertEquals(1, schMock.getDeliveredKeys().size());
        assertEquals(event.getEventKey(), schMock.getDeliveredKeys().get(0));
    }
}
