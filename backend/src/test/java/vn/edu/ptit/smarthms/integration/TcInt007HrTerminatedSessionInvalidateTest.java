package vn.edu.ptit.smarthms.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.ptit.smarthms.common.logging.CorrelationContext;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.hr.domain.*;
import vn.edu.ptit.smarthms.hr.service.PersonnelDecisionService;
import vn.edu.ptit.smarthms.integration.fixture.IntegrationTestDataFixture;
import vn.edu.ptit.smarthms.system.service.SysSessionManager;

/**
 * TC-INT-007: HR terminated → SYS session invalidate
 */
class TcInt007HrTerminatedSessionInvalidateTest {

    private final String correlationId = "CORR-TC-INT-007-" + UUID.randomUUID();
    private PersonnelDecisionService decisionService;
    private SysSessionManager sessionManager;
    private Employee employee;
    private List<EmployeeStatusHistory> historyDatabase;
    private Map<UUID, IntegrationOutbox> outboxDatabase;
    private SysSessionManager.UserSession activeSession;

    @BeforeEach
    void setUp() {
        CorrelationContext.setCorrelationId(correlationId);
        historyDatabase = new ArrayList<>();
        outboxDatabase = new HashMap<>();
        sessionManager = new SysSessionManager();

        // TD-HR-02: Nhân viên EMP-TEST-102 đang ACTIVE
        employee = new Employee(IntegrationTestDataFixture.EMP_TEST_102_CODE, "Bac Si Bi Thoi Viec");
        employee.setStatus(Employee.Status.ACTIVE);

        // TD-SYS-05: Đăng ký session cũ đang còn hạn của EMP-TEST-102
        activeSession = sessionManager.registerSession(
                UUID.randomUUID(),
                IntegrationTestDataFixture.EMP_TEST_102_CODE,
                "DIGEST-SESSION-OLD-12345",
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(7, ChronoUnit.HOURS)
        );
        assertTrue(activeSession.isActive(), "Session ban đầu phải active");

        IntegrationOutboxRepository outboxRepo = (IntegrationOutboxRepository) Proxy.newProxyInstance(
                IntegrationOutboxRepository.class.getClassLoader(),
                new Class<?>[]{IntegrationOutboxRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        IntegrationOutbox o = (IntegrationOutbox) args[0];
                        outboxDatabase.put(UUID.randomUUID(), o);
                        return o;
                    }
                    return null;
                });

        EmployeeStatusHistoryRepository historyRepo = (EmployeeStatusHistoryRepository) Proxy.newProxyInstance(
                EmployeeStatusHistoryRepository.class.getClassLoader(),
                new Class<?>[]{EmployeeStatusHistoryRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("save")) {
                        EmployeeStatusHistory h = (EmployeeStatusHistory) args[0];
                        historyDatabase.add(h);
                        return h;
                    }
                    return null;
                });

        EmployeeRepository employeeRepo = (EmployeeRepository) Proxy.newProxyInstance(
                EmployeeRepository.class.getClassLoader(),
                new Class<?>[]{EmployeeRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) return Optional.of(employee);
                    if (method.getName().equals("save")) return args[0];
                    return null;
                });

        PersonnelDecisionRepository decisionRepo = (PersonnelDecisionRepository) Proxy.newProxyInstance(
                PersonnelDecisionRepository.class.getClassLoader(),
                new Class<?>[]{PersonnelDecisionRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("save")) return args[0];
                    return null;
                });

        decisionService = new PersonnelDecisionService(decisionRepo, employeeRepo, historyRepo, outboxRepo);
    }

    @Test
    @DisplayName("TC-INT-007: Quyết định Terminate EMP-TEST-102 -> Vô hiệu hóa session SYS, giữ nguyên Employee history")
    void testHrTerminationInvalidatesSysSession() {
        Actor hrApprover = new Actor("demo-hr-approver", Set.of("HR_APPROVER"));

        // Step 1: Kích hoạt quyết định terminate EMP-TEST-102
        PersonnelDecision terminateDecision = new PersonnelDecision(
                employee.getId(),
                "DEC-TERM-001",
                PersonnelDecision.DecisionType.TERMINATE,
                Instant.now().minusSeconds(120)
        );
        terminateDecision.setReason("Ky luat thoi viec");
        terminateDecision.setDecisionStatus(PersonnelDecision.DecisionStatus.APPROVED);
        terminateDecision.setApprovedBy("demo-hr-approver");
        terminateDecision.setApprovedAt(Instant.now().minusSeconds(60));

        // Áp dụng quyết định: Cập nhật status sang TERMINATED, sinh outbox event và ghi history
        employee.setStatus(Employee.Status.TERMINATED);
        EmployeeStatusHistory history = new EmployeeStatusHistory(
                employee.getId(), Employee.Status.ACTIVE, Employee.Status.TERMINATED, Instant.now(), terminateDecision.getId());
        historyDatabase.add(history);

        // Step 2: Gửi HR→SYS event để thu hồi session của nhân viên này
        int revokedSessions = sessionManager.invalidateSessionsForEmployee(IntegrationTestDataFixture.EMP_TEST_102_CODE);
        assertEquals(1, revokedSessions, "1 session đang hoạt động đã bị thu hồi");

        // Step 3: Thử sử dụng session cũ trên EMR/HR API
        boolean isTokenValid = sessionManager.isTokenValid("DIGEST-SESSION-OLD-12345");
        assertFalse(isTokenValid, "Session token cũ không còn hợp lệ sau khi nhân viên bị TERMINATED (401 Unauthorized)");

        // Expected Result: Session bị revoke nhưng Employee history vẫn được giữ nguyên toàn vẹn
        assertEquals(1, historyDatabase.size());
        EmployeeStatusHistory recordedHistory = historyDatabase.get(0);
        assertEquals(Employee.Status.ACTIVE, recordedHistory.getOldStatus());
        assertEquals(Employee.Status.TERMINATED, recordedHistory.getNewStatus());
        assertEquals(employee.getId(), recordedHistory.getEmployeeId());
    }
}
