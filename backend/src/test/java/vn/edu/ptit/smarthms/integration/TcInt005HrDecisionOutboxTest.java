package vn.edu.ptit.smarthms.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.ptit.smarthms.common.logging.CorrelationContext;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.hr.api.HrResponses;
import vn.edu.ptit.smarthms.hr.domain.*;
import vn.edu.ptit.smarthms.hr.service.PersonnelDecisionService;
import vn.edu.ptit.smarthms.hr.service.SchIntegrationDispatcher;
import vn.edu.ptit.smarthms.integration.fixture.IntegrationTestDataFixture;
import vn.edu.ptit.smarthms.integration.mock.MockSchedulerClient;

/**
 * TC-INT-005: HR effective decision → outbox
 */
class TcInt005HrDecisionOutboxTest {

    private final String correlationId = "CORR-TC-INT-005-" + UUID.randomUUID();
    private PersonnelDecisionService decisionService;
    private SchIntegrationDispatcher dispatcher;
    private MockSchedulerClient schMock;
    private Map<UUID, IntegrationOutbox> outboxDatabase;
    private Map<UUID, PersonnelDecision> decisionDatabase;
    private Employee employee;

    @BeforeEach
    void setUp() {
        CorrelationContext.setCorrelationId(correlationId);
        outboxDatabase = new HashMap<>();
        decisionDatabase = new HashMap<>();
        schMock = new MockSchedulerClient();

        employee = new Employee("EMP-TEST-005", "Nguyen Van Chuyen Chuc");
        employee.setStatus(Employee.Status.ACTIVE);

        IntegrationOutboxRepository outboxRepo = (IntegrationOutboxRepository) Proxy.newProxyInstance(
                IntegrationOutboxRepository.class.getClassLoader(),
                new Class<?>[]{IntegrationOutboxRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("save")) {
                        IntegrationOutbox o = (IntegrationOutbox) args[0];
                        outboxDatabase.put(o.getId() != null ? o.getId() : UUID.randomUUID(), o);
                        return o;
                    }
                    if (name.equals("existsByEventKey")) {
                        String key = (String) args[0];
                        return outboxDatabase.values().stream().anyMatch(o -> o.getEventKey().equals(key));
                    }
                    if (name.equals("findByDeliveryState")) {
                        IntegrationOutbox.DeliveryState s = (IntegrationOutbox.DeliveryState) args[0];
                        return outboxDatabase.values().stream()
                                .filter(o -> o.getDeliveryState() == s)
                                .toList();
                    }
                    return null;
                });

        PersonnelDecisionRepository decisionRepo = (PersonnelDecisionRepository) Proxy.newProxyInstance(
                PersonnelDecisionRepository.class.getClassLoader(),
                new Class<?>[]{PersonnelDecisionRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("save")) {
                        PersonnelDecision d = (PersonnelDecision) args[0];
                        decisionDatabase.put(d.getId() != null ? d.getId() : UUID.randomUUID(), d);
                        return d;
                    }
                    if (name.equals("findByDecisionStatusAndExecutionStatusAndEffectiveAtLessThanEqual")) {
                        return decisionDatabase.values().stream()
                                .filter(d -> d.getDecisionStatus() == PersonnelDecision.DecisionStatus.APPROVED
                                        && d.getExecutionStatus() == PersonnelDecision.ExecutionStatus.PENDING)
                                .toList();
                    }
                    return null;
                });

        EmployeeRepository employeeRepo = (EmployeeRepository) Proxy.newProxyInstance(
                EmployeeRepository.class.getClassLoader(),
                new Class<?>[]{EmployeeRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        return Optional.of(employee);
                    }
                    if (method.getName().equals("save")) {
                        return args[0];
                    }
                    return null;
                });

        EmployeeStatusHistoryRepository historyRepo = (EmployeeStatusHistoryRepository) Proxy.newProxyInstance(
                EmployeeStatusHistoryRepository.class.getClassLoader(),
                new Class<?>[]{EmployeeStatusHistoryRepository.class},
                (proxy, method, args) -> args[0]);

        decisionService = new PersonnelDecisionService(decisionRepo, employeeRepo, historyRepo, outboxRepo);
        dispatcher = new SchIntegrationDispatcher(outboxRepo);
    }

    @Test
    @DisplayName("TC-INT-005: HR commit decision hiệu lực -> Tạo outbox liên kết decision; chưa báo đồng bộ ngoài cho đến khi delivery")
    void testHrDecisionToOutboxAndDelivery() {
        Actor approver = new Actor("demo-hr-approver", Set.of("HR_APPROVER"));

        UUID employeeId = UUID.randomUUID();
        // Step 1: Ghi DEC-TEST-001 hiệu lực quá khứ (due) và approved
        PersonnelDecision decision = new PersonnelDecision(
                employeeId,
                IntegrationTestDataFixture.DEC_TEST_001_CODE,
                PersonnelDecision.DecisionType.TERMINATE,
                Instant.now().minusSeconds(600) // đã có hiệu lực
        );
        decision.setReason("Cham dut hop dong lao dong");
        decision.setDecisionStatus(PersonnelDecision.DecisionStatus.APPROVED);
        decision.setApprovedBy(approver.principalName());
        decision.setApprovedAt(Instant.now().minusSeconds(300));
        decisionDatabase.put(decision.getId() != null ? decision.getId() : UUID.randomUUID(), decision);

        // Áp dụng quyết định (commit transaction)
        List<HrResponses.DecisionResponse> applied = decisionService.applyDueDecisions(approver);
        assertEquals(1, applied.size());
        assertEquals(PersonnelDecision.ExecutionStatus.APPLIED, decision.getExecutionStatus());

        // Step 2: Kiểm cùng transaction có event outbox liên kết decision
        assertEquals(1, outboxDatabase.size(), "Bảng outbox phải có đúng 1 record event được sinh ra");
        IntegrationOutbox outboxEvent = outboxDatabase.values().iterator().next();
        assertTrue(outboxEvent.getEventKey().contains(employeeId.toString()),
                "Event key trỏ đúng EmployeeId");
        assertTrue(outboxEvent.getEventKey().contains("EMPLOYEE_TERMINATED"),
                "Event type khớp với quyết định TERMINATE");

        // Expected: Trước khi delivery, trạng thái outbox là PENDING (chưa báo đồng bộ ngoài hệ thống)
        assertEquals(IntegrationOutbox.DeliveryState.PENDING, outboxEvent.getDeliveryState());
        assertEquals(0, outboxEvent.getAttempts());
        assertTrue(schMock.getDeliveredKeys().isEmpty());

        // Step 3: Delivery tới SYS/SCH mock rồi so sánh log
        SchIntegrationDispatcher.DispatchResult result = dispatcher.dispatchPendingEvents(schMock);
        assertEquals(1, result.successCount());
        assertEquals(0, result.failureCount());

        // Sau khi delivery thành công: Trạng thái đổi thành SENT
        assertEquals(IntegrationOutbox.DeliveryState.SENT, outboxEvent.getDeliveryState());
        assertEquals(1, outboxEvent.getAttempts());
        assertNotNull(outboxEvent.getLastAttemptAt());
        assertTrue(schMock.getDeliveredKeys().contains(outboxEvent.getEventKey()),
                "Downstream SCH mock nhận được đúng eventKey");
    }
}
