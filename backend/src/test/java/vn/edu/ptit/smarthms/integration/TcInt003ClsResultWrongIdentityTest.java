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
 * TC-INT-003: CLS result wrong identity
 */
class TcInt003ClsResultWrongIdentityTest {

    private final String correlationId = "CORR-TC-INT-003-" + UUID.randomUUID();
    private ClsIntegrationService clsService;
    private Map<UUID, ClinicalResult> resultDatabase;
    private MedicalEncounter trueEncounter;
    private MedicalOrder trueOrder;
    private UUID wrongPatientId;

    @BeforeEach
    void setUp() {
        CorrelationContext.setCorrelationId(correlationId);
        resultDatabase = new HashMap<>();
        wrongPatientId = UUID.fromString("99999999-9999-9999-9999-999999999999");

        // Step 1: Tạo encounter và order cho PAT-TEST-001
        trueEncounter = MedicalEncounter.create(
                IntegrationTestDataFixture.PAT_TEST_001,
                IntegrationTestDataFixture.APT_TEST_001,
                IntegrationTestDataFixture.DOC_TEST_001_ID,
                IntegrationTestDataFixture.DEPT_TEST_001_ID,
                "DOC-TEST-001"
        );

        trueOrder = MedicalOrder.create(
                trueEncounter.getEncounterId(),
                IntegrationTestDataFixture.DOC_TEST_001_ID,
                MedicalOrder.Type.LAB,
                "EXT-ORD-001",
                "DOC-TEST-001"
        );

        MedicalOrderRepository orderRepo = (MedicalOrderRepository) Proxy.newProxyInstance(
                MedicalOrderRepository.class.getClassLoader(),
                new Class<?>[]{MedicalOrderRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        UUID id = (UUID) args[0];
                        if (trueOrder.getOrderId().equals(id)) {
                            return Optional.of(trueOrder);
                        }
                        return Optional.empty();
                    }
                    return null;
                });

        MedicalEncounterRepository encRepo = (MedicalEncounterRepository) Proxy.newProxyInstance(
                MedicalEncounterRepository.class.getClassLoader(),
                new Class<?>[]{MedicalEncounterRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        UUID id = (UUID) args[0];
                        if (trueEncounter.getEncounterId().equals(id)) {
                            return Optional.of(trueEncounter);
                        }
                        return Optional.empty();
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
                    if (name.equals("countByQuality")) {
                        String q = (String) args[0];
                        return resultDatabase.values().stream().filter(r -> r.getQuality().equals(q)).count();
                    }
                    if (name.equals("countByOrderId")) {
                        UUID ordId = (UUID) args[0];
                        return resultDatabase.values().stream()
                                .filter(r -> r.getOrderId().equals(ordId) && !r.isQuarantined())
                                .count();
                    }
                    return null;
                });

        clsService = new ClsIntegrationService(resultRepo, orderRepo, encRepo);
    }

    @Test
    @DisplayName("TC-INT-003: CLS mock gửi kết quả sai PID -> Cách ly (Quarantine), không gắn vào hồ sơ bệnh nhân")
    void testClsResultWrongIdentityQuarantine() {
        // Step 2: CLS mock gửi result chứa OrderID của PAT-TEST-001 nhưng sai PID (TD-CLS-02)
        ClsIntegrationService.ClsEventMessage wrongIdentityEvent = new ClsIntegrationService.ClsEventMessage(
                "EV-CLS-ERR-001",
                IntegrationTestDataFixture.CLS_SOURCE_SYSTEM,
                "RES-001",
                "1.0",
                trueOrder.getOrderId(),
                wrongPatientId, // Sai PID
                OffsetDateTime.now(),
                "{\"glucose\": 5.5, \"wbc\": 6.2}",
                false
        );

        ClsIntegrationService.IngestOutcome outcome = clsService.ingestResult(wrongIdentityEvent, "MOCK-CLS-DISPATCHER");

        // Step 3: Query result count trên hồ sơ đúng/sai và quarantine
        assertFalse(outcome.accepted(), "Bản tin không được chấp nhận vào hồ sơ hợp lệ");
        assertTrue(outcome.quarantined(), "Bản tin phải bị cách ly (Quarantine)");

        // Bản chụp DB: Có 1 record QUARANTINED, 0 record VALID
        assertEquals(1, resultDatabase.size());
        ClinicalResult stored = resultDatabase.values().iterator().next();
        assertEquals(ClinicalResult.Quality.QUARANTINED.name(), stored.getQuality(),
                "Trạng thái đối soát là QUARANTINED");

        // Order chính không bị đổi sang COMPLETED do bị mismatch
        assertNotEquals(MedicalOrder.Status.COMPLETED, trueOrder.getOrderStatus(),
                "Order không được đánh dấu hoàn thành khi result bị sai identity");
        assertEquals(correlationId, CorrelationContext.getCorrelationId());
    }

    @Test
    @DisplayName("TC-INT-003b: CLS mock gửi kết quả sai OrderID hoàn toàn -> Không gắn vào bất kỳ hồ sơ nào")
    void testClsResultNonExistentOrderQuarantine() {
        UUID nonExistentOrderId = UUID.randomUUID();
        ClsIntegrationService.ClsEventMessage invalidOrderEvent = new ClsIntegrationService.ClsEventMessage(
                "EV-CLS-ERR-002",
                IntegrationTestDataFixture.CLS_SOURCE_SYSTEM,
                "RES-002",
                "1.0",
                nonExistentOrderId,
                IntegrationTestDataFixture.PAT_TEST_001,
                OffsetDateTime.now(),
                "{\"blood_type\": \"O+\"}",
                false
        );

        ClsIntegrationService.IngestOutcome outcome = clsService.ingestResult(invalidOrderEvent, "MOCK-CLS-DISPATCHER");
        assertTrue(outcome.quarantined());
        assertFalse(outcome.accepted());
        assertEquals(0, resultDatabase.size(), "Không insert record sai Foreign Key vào bảng kết quả lâm sàng");
    }
}
