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
import vn.edu.ptit.smarthms.integration.fixture.IntegrationTestDataFixture;

/**
 * TC-INT-004: CLS replay/delay
 */
class TcInt004ClsReplayDelayTest {

    private final String correlationId = "CORR-TC-INT-004-" + UUID.randomUUID();
    private ClsIntegrationService clsService;
    private Map<UUID, ClinicalResult> resultDatabase;
    private MedicalEncounter encounter;
    private MedicalOrder order;

    @BeforeEach
    void setUp() {
        CorrelationContext.setCorrelationId(correlationId);
        resultDatabase = new HashMap<>();

        encounter = MedicalEncounter.create(
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
                "ORD-TEST-004",
                "DOC-TEST-001"
        );

        MedicalOrderRepository orderRepo = (MedicalOrderRepository) Proxy.newProxyInstance(
                MedicalOrderRepository.class.getClassLoader(),
                new Class<?>[]{MedicalOrderRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        return Optional.of(order);
                    }
                    return null;
                });

        MedicalEncounterRepository encRepo = (MedicalEncounterRepository) Proxy.newProxyInstance(
                MedicalEncounterRepository.class.getClassLoader(),
                new Class<?>[]{MedicalEncounterRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        return Optional.of(encounter);
                    }
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
    @DisplayName("TC-INT-004: CLS mock gửi EV-CLS-001 2 lần và gửi chậm -> Idempotency, chỉ 1 logical result, bảo toàn provenance/version")
    void testClsReplayAndDelay() {
        OffsetDateTime originalResultTime = OffsetDateTime.now().minusMinutes(10);
        String eventPayload = "{\"platelets\": 250, \"rbc\": 4.5}";

        // Step 1a: Lần gửi thứ nhất
        ClsIntegrationService.ClsEventMessage event1 = new ClsIntegrationService.ClsEventMessage(
                IntegrationTestDataFixture.EV_CLS_001,
                IntegrationTestDataFixture.CLS_SOURCE_SYSTEM,
                "RES-TD-CLS-03",
                "1.0",
                order.getOrderId(),
                IntegrationTestDataFixture.PAT_TEST_001,
                originalResultTime,
                eventPayload,
                false
        );
        ClsIntegrationService.IngestOutcome outcome1 = clsService.ingestResult(event1, "CLS-DISPATCHER");
        assertTrue(outcome1.accepted());
        assertFalse(outcome1.isDuplicate());
        assertEquals(1, resultDatabase.size());

        // Step 1b: Lần gửi thứ 2 (Replay tức thời cùng eventId/sourceResultId)
        ClsIntegrationService.IngestOutcome outcome2 = clsService.ingestResult(event1, "CLS-DISPATCHER");
        assertTrue(outcome2.accepted(), "Replay được chấp nhận dưới dạng idempotent");
        assertTrue(outcome2.isDuplicate(), "Hệ thống phát hiện bản tin trùng lặp");
        assertEquals(1, resultDatabase.size(), "Không tạo thêm bản ghi nào trong DB");

        // Step 1c: Lần gửi thứ 3 (Gửi chậm - delayed after network partition)
        ClsIntegrationService.ClsEventMessage eventDelayed = new ClsIntegrationService.ClsEventMessage(
                IntegrationTestDataFixture.EV_CLS_001,
                IntegrationTestDataFixture.CLS_SOURCE_SYSTEM,
                "RES-TD-CLS-03",
                "1.0",
                order.getOrderId(),
                IntegrationTestDataFixture.PAT_TEST_001,
                originalResultTime, // giữ nguyên provenance timestamp
                eventPayload,
                false
        );
        ClsIntegrationService.IngestOutcome outcomeDelayed = clsService.ingestResult(eventDelayed, "CLS-DISPATCHER");
        assertTrue(outcomeDelayed.isDuplicate());
        assertEquals(1, resultDatabase.size(), "DB chỉ có đúng 1 logical result duy nhất");

        // Step 2: Tra theo eventId/source, kiểm version và counts
        ClinicalResult stored = resultDatabase.values().iterator().next();
        assertEquals("RES-TD-CLS-03", stored.getSourceResultId());
        assertEquals(IntegrationTestDataFixture.CLS_SOURCE_SYSTEM, stored.getSourceSystem());
        assertEquals("1.0", stored.getSourceVersion());
        assertEquals(originalResultTime, stored.getResultAt(), "Provenance time không bị ghi đè hay mất mát");
        assertEquals(ClinicalResult.Quality.VALID.name(), stored.getQuality());
    }
}
