package vn.edu.ptit.smarthms.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.common.logging.CorrelationContext;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.domain.*;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.repository.DiagnosisRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.service.DiagnosisService;
import vn.edu.ptit.smarthms.integration.fixture.IntegrationTestDataFixture;

/**
 * TC-INT-002: HR credential guard → EMR
 */
class TcInt002HrCredentialGuardTest {

    private final String correlationId = "CORR-TC-INT-002-" + UUID.randomUUID();
    private Actor doctorActor;
    private DiagnosisService diagnosisService;
    private Map<UUID, Diagnosis> diagnosisDatabase;
    private MedicalEncounter existingEncounter;

    @BeforeEach
    void setUp() {
        CorrelationContext.setCorrelationId(correlationId);
        // Bác sĩ DOC-TEST-001 có SYS role DOCTOR (TD-SYS-01)
        doctorActor = new Actor(IntegrationTestDataFixture.DOC_TEST_001_CODE, Set.of("DOCTOR"));
        diagnosisDatabase = new HashMap<>();

        existingEncounter = MedicalEncounter.create(
                IntegrationTestDataFixture.PAT_TEST_001,
                IntegrationTestDataFixture.APT_TEST_001,
                IntegrationTestDataFixture.DOC_TEST_001_ID,
                IntegrationTestDataFixture.DEPT_TEST_001_ID,
                doctorActor.principalName()
        );
        // Đã ở trạng thái IN_PROGRESS
        existingEncounter.changeStatus(EncounterStatus.IN_PROGRESS, doctorActor.principalName(), java.time.OffsetDateTime.now());

        MedicalEncounterRepository encounterRepo = (MedicalEncounterRepository) Proxy.newProxyInstance(
                MedicalEncounterRepository.class.getClassLoader(),
                new Class<?>[]{MedicalEncounterRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findById")) {
                        return Optional.of(existingEncounter);
                    }
                    return null;
                });

        DiagnosisRepository diagnosisRepo = (DiagnosisRepository) Proxy.newProxyInstance(
                DiagnosisRepository.class.getClassLoader(),
                new Class<?>[]{DiagnosisRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("existsByEncounterIdAndDiagnosisType")) {
                        return false;
                    }
                    if (name.equals("save")) {
                        Diagnosis d = (Diagnosis) args[0];
                        diagnosisDatabase.put(d.getDiagnosisId(), d);
                        return d;
                    }
                    return null;
                });

        // TD-HR-03: HR certificate (CCHN) hết hiệu lực (hasVerifiedLicense = false)
        ReferenceLookupPort lookupProxy = (ReferenceLookupPort) Proxy.newProxyInstance(
                ReferenceLookupPort.class.getClassLoader(),
                new Class<?>[]{ReferenceLookupPort.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getPractitionerDetails")) {
                        return Optional.of(IntegrationTestDataFixture.expiredLicensePractitioner());
                    }
                    return null;
                });

        diagnosisService = new DiagnosisService(diagnosisRepo, encounterRepo, lookupProxy);
    }

    @Test
    @DisplayName("TC-INT-002: Bác sĩ có SYS role nhưng HR CCHN hết hiệu lực -> Backend từ chối nghiệp vụ chẩn đoán EMR, không sửa DB")
    void testHrCredentialGuardRejectsExpiredLicense() {
        // Step 1 & 2: Thử gọi thao tác chẩn đoán EMR khi CCHN hết hạn
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                diagnosisService.create(
                        doctorActor,
                        existingEncounter.getEncounterId(),
                        DiagnosisType.PRIMARY,
                        "J00",
                        "Viêm mũi họng cấp"
                ));

        // Step 3: Đối chiếu access decision, version và audit
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode(),
                "Backend từ chối nghiệp vụ không đủ tư cách (422 UNPROCESSABLE_ENTITY)");
        assertTrue(ex.getReason().contains("license") || ex.getReason().contains("CCHN"),
                "Thông báo từ chối nêu rõ nguyên nhân thiếu CCHN hợp lệ");

        // Expected Result: Không sửa EMR, không lưu thêm chẩn đoán nào vào DB
        assertTrue(diagnosisDatabase.isEmpty(), "Bản chụp DB EMR không bị thêm hoặc sửa đổi");

        // Không tự giả định đình chỉ SYS account: Role DOCTOR của actor vẫn được giữ nguyên
        assertTrue(doctorActor.hasRole("DOCTOR"), "SYS role của tài khoản không bị tự ý xóa hay đình chỉ");
        assertEquals(correlationId, CorrelationContext.getCorrelationId());
    }
}
