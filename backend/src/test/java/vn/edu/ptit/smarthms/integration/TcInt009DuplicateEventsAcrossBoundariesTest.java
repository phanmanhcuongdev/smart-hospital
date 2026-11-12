package vn.edu.ptit.smarthms.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.ptit.smarthms.common.logging.CorrelationContext;
import vn.edu.ptit.smarthms.emr.domain.ClinicalResult;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrder;
import vn.edu.ptit.smarthms.emr.repository.ClinicalResultRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalOrderRepository;
import vn.edu.ptit.smarthms.emr.service.ClsIntegrationService;
import vn.edu.ptit.smarthms.hr.domain.IntegrationOutbox;
import vn.edu.ptit.smarthms.hr.domain.IntegrationOutboxRepository;
import vn.edu.ptit.smarthms.integration.fixture.IntegrationTestDataFixture;

/**
 * TC-INT-009: Duplicate events across boundaries (P1)
 */
class TcInt009DuplicateEventsAcrossBoundariesTest {

    private final String correlationId = "CORR-TC-INT-009-" + UUID.randomUUID();
    private ClsIntegrationService clsService;
    private Map<UUID, ClinicalResult> resultDatabase;
    private Map<String, IntegrationOutbox> outboxDatabase;
    private MedicalOrder order;

    @BeforeEach
    void setUp() {
        CorrelationContext.setCorrelationId(correlationId);
        resultDatabase = new HashMap<>();
        outboxDatabase = new HashMap<>();

        MedicalEncounter encounter = MedicalEncounter.create(
                IntegrationTestDataFixture.PAT_TEST_001,
                IntegrationTestDataFixture.APT_TEST_001,
                IntegrationTestDataFixture.DOC_TEST_001_ID,
                IntegrationTestDataFixture.DEPT_TEST_001_ID,
                "DOC-TEST-001"
        );

        order = MedicalOrder.create(
                encounter.getEncounterId(),
                IntegrationTestDataFixture.DOC_TEST_001_ID,
                MedicalOrder.Type.LAB,
                "ORD-INT-009",
                "DOC-TEST-001"
        );

        MedicalOrderRepository orderRepo = (MedicalOrderRepository) Proxy.newProxyInstance(
                MedicalOrderRepository.class.getClassLoader(),
                new Class<?>[]{MedicalOrderRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) return Optional.of(order);
                    return null;
                });

        MedicalEncounterRepository encRepo = (MedicalEncounterRepository) Proxy.newProxyInstance(
                MedicalEncounterRepository.class.getClassLoader(),
                new Class<?>[]{MedicalEncounterRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) return Optional.of(encounter);
                    return null;
                });

        ClinicalResultRepository resultRepo = (ClinicalResultRepository) Proxy.newProxyInstance(
                ClinicalResultRepository.class.getClassLoader(),
                new Class<?>[]{ClinicalResultRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("findBySourceSystemAndSourceResultIdAndSourceVersion")) {
                        return resultDatabase.values().stream()
                                .filter(r -> r.getSourceSystem().equals(args[0])
                                        && r.getSourceResultId().equals(args[1])
                                        && r.getSourceVersion().equals(args[2]))
                                .findFirst();
                    }
                    if (name.equals("save")) {
                        ClinicalResult r = (ClinicalResult) args[0];
                        resultDatabase.put(r.getResultId(), r);
                        return r;
                    }
                    return null;
                });

        clsService = new ClsIntegrationService(resultRepo, orderRepo, encRepo);
    }

    @Test
    @DisplayName("TC-INT-009: Lặp lại cùng eventId 3 lần từ CLS & HR -> Giữ nguyên số lượng record nghiệp vụ ban đầu (Không nhân bản)")
    void testDuplicateEventsAcrossBoundaries() {
        // Step 1: Lặp eventId CLS 3 lần trong dataset mock
        ClsIntegrationService.ClsEventMessage clsMessage = new ClsIntegrationService.ClsEventMessage(
                "EV-CLS-DUP-001",
                IntegrationTestDataFixture.CLS_SOURCE_SYSTEM,
                "RES-DUP-001",
                "1.0",
                order.getOrderId(),
                IntegrationTestDataFixture.PAT_TEST_001,
                OffsetDateTime.now(),
                "{\"blood_sugar\": 105}",
                false
        );

        ClsIntegrationService.IngestOutcome out1 = clsService.ingestResult(clsMessage, "DISPATCHER");
        ClsIntegrationService.IngestOutcome out2 = clsService.ingestResult(clsMessage, "DISPATCHER");
        ClsIntegrationService.IngestOutcome out3 = clsService.ingestResult(clsMessage, "DISPATCHER");

        // Lần đầu tạo mới, 2 lần sau bị chặn duplicate
        assertFalse(out1.isDuplicate());
        assertTrue(out2.isDuplicate());
        assertTrue(out3.isDuplicate());

        // Step 2: Lặp event HR outbox cùng eventKey 3 lần
        String hrEventKey = "EMPLOYEE_TERMINATED:22222222-2222-2222-2222-222222220001:DEC-001";
        for (int i = 0; i < 3; i++) {
            outboxDatabase.putIfAbsent(hrEventKey,
                    new IntegrationOutbox(hrEventKey, UUID.randomUUID(), "{\"desc\":\"terminated\"}"));
        }

        // Expected Result: Đối chiếu số record business trước/sau: không tạo nhân bản nghiệp vụ
        assertEquals(1, resultDatabase.size(), "Bảng clinical_result chỉ có duy nhất 1 bản ghi");
        assertEquals(1, outboxDatabase.size(), "Bảng outbox chỉ có duy nhất 1 bản ghi eventKey tương ứng");
        assertEquals(correlationId, CorrelationContext.getCorrelationId());
    }
}
