package vn.edu.ptit.smarthms.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.ptit.smarthms.common.logging.CorrelationContext;
import vn.edu.ptit.smarthms.system.domain.AuditEvent;
import vn.edu.ptit.smarthms.system.repository.AuditEventRepository;
import vn.edu.ptit.smarthms.system.service.AuditService;

/**
 * TC-INT-008: Audit across EMR/HR
 */
class TcInt008AuditAcrossEmrHrTest {

    private final String correlationId = "CORR-TC-INT-008-" + UUID.randomUUID();
    private AuditService auditService;
    private Map<UUID, AuditEvent> auditDatabase;

    @BeforeEach
    void setUp() {
        CorrelationContext.setCorrelationId(correlationId);
        auditDatabase = new HashMap<>();

        AuditEventRepository auditRepo = (AuditEventRepository) Proxy.newProxyInstance(
                AuditEventRepository.class.getClassLoader(),
                new Class<?>[]{AuditEventRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("save")) {
                        AuditEvent a = (AuditEvent) args[0];
                        auditDatabase.put(a.getAuditId(), a);
                        return a;
                    }
                    if (name.equals("findByActorRefOrderByHappenedAtDesc")) {
                        String actor = (String) args[0];
                        return auditDatabase.values().stream()
                                .filter(a -> a.getActorRef().equals(actor))
                                .toList();
                    }
                    if (name.equals("findByOutcome")) {
                        String outcome = (String) args[0];
                        return auditDatabase.values().stream()
                                .filter(a -> a.getOutcome().equals(outcome))
                                .toList();
                    }
                    return null;
                });

        auditService = new AuditService(auditRepo);
    }

    @Test
    @DisplayName("TC-INT-008: Ghi audit cho thay đổi HR, EMR và hành vi trái quyền; Che giấu token, password và dữ liệu PHI")
    void testAuditAcrossEmrHrAndSanitization() {
        UUID docUserId = UUID.randomUUID();
        UUID hrUserId = UUID.randomUUID();
        UUID attackerUserId = UUID.randomUUID();

        // Step 1a: Thực hiện một thay đổi HR hợp lệ (Duyệt quyết định)
        String hrMetadataWithSensitiveInfo = "{\"decisionId\":\"DEC-TEST-001\",\"action\":\"APPROVE\",\"token\":\"secret-bearer-token-12345\"}";
        auditService.record(
                hrUserId,
                "demo-hr-approver",
                "hr.personnel_decision",
                "DEC-TEST-001",
                "APPROVE_DECISION",
                AuditEvent.Outcome.SUCCESS,
                "REF-DEC-001",
                hrMetadataWithSensitiveInfo
        );

        // Step 1b: Thực hiện một thay đổi EMR hợp lệ (Chẩn đoán bệnh)
        String emrMetadata = "{\"encounterId\":\"ENC-TEST-001\",\"icd10\":\"J00\",\"diagnosis\":\"Acute nasopharyngitis\"}";
        auditService.record(
                docUserId,
                "DOC-TEST-001",
                "emr.diagnosis",
                "DIAG-001",
                "CREATE_DIAGNOSIS",
                AuditEvent.Outcome.SUCCESS,
                "ENC-TEST-001",
                emrMetadata
        );

        // Step 2: Thử một hành động trái quyền (Lễ tân cố truy cập sửa hồ sơ EMR và bị từ chối)
        String unauthorizedMetadata = "{\"encounterId\":\"ENC-TEST-001\",\"attemptedRole\":\"RECEPTION\",\"password\":\"my-plain-pass\"}";
        auditService.record(
                attackerUserId,
                "demo-reception",
                "emr.medical_record",
                "ENC-TEST-001",
                "LOCK_RECORD",
                AuditEvent.Outcome.DENIED,
                "ATTEMPT-DENIED-001",
                unauthorizedMetadata
        );

        // Step 3: Query audit theo actor/object/action/result/time/source
        List<AuditEvent> successfulEvents = auditService.queryByOutcome(AuditEvent.Outcome.SUCCESS);
        List<AuditEvent> deniedEvents = auditService.queryByOutcome(AuditEvent.Outcome.DENIED);

        assertEquals(2, successfulEvents.size(), "Có 2 audit event thành công (1 HR + 1 EMR)");
        assertEquals(1, deniedEvents.size(), "Có 1 audit event bị từ chối do vi phạm quyền");

        // Expected Result: Kiểm tra che giấu thông tin nhạy cảm (Sanitization)
        AuditEvent hrAudit = successfulEvents.stream()
                .filter(a -> a.getActorRef().equals("demo-hr-approver"))
                .findFirst().orElseThrow();
        assertFalse(hrAudit.getSafeMetadata().contains("secret-bearer-token-12345"),
                "Token bí mật không được xuất hiện trong audit safe_metadata");
        assertTrue(hrAudit.getSafeMetadata().contains("[REDACTED]"),
                "Token đã được thay thế bằng [REDACTED]");

        AuditEvent deniedAudit = deniedEvents.get(0);
        assertFalse(deniedAudit.getSafeMetadata().contains("my-plain-pass"),
                "Mật khẩu không được xuất hiện trong audit log");
        assertTrue(deniedAudit.getSafeMetadata().contains("[REDACTED]"));
    }
}
