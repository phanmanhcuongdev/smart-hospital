package vn.edu.ptit.smarthms.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.ptit.smarthms.common.logging.CorrelationContext;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalRecordRepository;
import vn.edu.ptit.smarthms.emr.service.EncounterService;
import vn.edu.ptit.smarthms.integration.fixture.IntegrationTestDataFixture;

/**
 * TC-INT-001: D4→D5: Encounter fixture → EMR
 */
class TcInt001EncounterFixtureLinkageTest {

    private final String correlationId = "CORR-TC-INT-001-" + UUID.randomUUID();
    private Actor doctorActor;
    private EncounterService encounterService;
    private Map<UUID, MedicalEncounter> encounterDatabase;
    private Map<UUID, MedicalRecord> recordDatabase;

    @BeforeEach
    void setUp() {
        CorrelationContext.setCorrelationId(correlationId);
        doctorActor = new Actor(IntegrationTestDataFixture.DOC_TEST_001_CODE, Set.of("DOCTOR"));

        encounterDatabase = new HashMap<>();
        recordDatabase = new HashMap<>();

        // Repository mock backed by map (isolated DB snapshot)
        MedicalEncounterRepository repoProxy = (MedicalEncounterRepository) Proxy.newProxyInstance(
                MedicalEncounterRepository.class.getClassLoader(),
                new Class<?>[]{MedicalEncounterRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("saveAndFlush") || name.equals("save")) {
                        MedicalEncounter e = (MedicalEncounter) args[0];
                        encounterDatabase.put(e.getEncounterId(), e);
                        return e;
                    }
                    if (name.equals("findById")) {
                        return Optional.ofNullable(encounterDatabase.get(args[0]));
                    }
                    return null;
                });

        MedicalRecordRepository recordProxy = (MedicalRecordRepository) Proxy.newProxyInstance(
                MedicalRecordRepository.class.getClassLoader(),
                new Class<?>[]{MedicalRecordRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("findByEncounterId")) {
                        return recordDatabase.values().stream()
                                .filter(r -> r.getEncounterId().equals(args[0]))
                                .findFirst();
                    }
                    if (name.equals("save")) {
                        MedicalRecord r = (MedicalRecord) args[0];
                        recordDatabase.put(r.getRecordId(), r);
                        return r;
                    }
                    return null;
                });

        // Fixture adapter for ReferenceLookupPort (D4 adapter)
        ReferenceLookupPort lookupProxy = (ReferenceLookupPort) Proxy.newProxyInstance(
                ReferenceLookupPort.class.getClassLoader(),
                new Class<?>[]{ReferenceLookupPort.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("patientExists")) {
                        // Patient exists in fixture only
                        return args[0].equals(IntegrationTestDataFixture.PAT_TEST_001);
                    }
                    if (name.equals("practitionerExists")) {
                        return args[0].equals(IntegrationTestDataFixture.DOC_TEST_001_ID);
                    }
                    if (name.equals("getPractitionerDetails")) {
                        return Optional.of(IntegrationTestDataFixture.validPractitioner());
                    }
                    if (name.equals("departmentExists") || name.equals("isDepartmentActive")) {
                        return true;
                    }
                    return null;
                });

        encounterService = new EncounterService(repoProxy, recordProxy, lookupProxy);
    }

    @Test
    @DisplayName("TC-INT-001: Mở encounter từ fixture Patient/Appointment, verify IDs và không nhân bản")
    void testEncounterFixtureToEmr() {
        // Step 1 & 2: Dưới quyền DOC-TEST-001 mở encounter cho PAT-TEST-001 và APT-TEST-001
        MedicalEncounter created = encounterService.create(
                doctorActor,
                IntegrationTestDataFixture.PAT_TEST_001,
                IntegrationTestDataFixture.APT_TEST_001,
                IntegrationTestDataFixture.DOC_TEST_001_ID,
                IntegrationTestDataFixture.DEPT_TEST_001_ID
        );

        assertNotNull(created);
        assertNotNull(created.getEncounterId());
        assertEquals(EncounterStatus.WAITING, created.getStatus());

        // Chuyển sang IN_PROGRESS để lưu nháp MedicalRecord
        MedicalEncounter inProgress = encounterService.changeStatus(
                doctorActor, created.getEncounterId(), EncounterStatus.IN_PROGRESS);
        assertEquals(EncounterStatus.IN_PROGRESS, inProgress.getStatus());

        // Step 3: So sánh toàn bộ ID ở fixture và EMR
        assertEquals(IntegrationTestDataFixture.PAT_TEST_001, inProgress.getPatientId(),
                "Encounter phải gắn đúng một PID từ fixture");
        assertEquals(IntegrationTestDataFixture.APT_TEST_001, inProgress.getAppointmentId(),
                "Encounter phải gắn đúng Appointment ID từ fixture");
        assertEquals(IntegrationTestDataFixture.DOC_TEST_001_ID, inProgress.getAttendingEmployeeId(),
                "Encounter phải gắn đúng Bác sĩ phụ trách");
        assertEquals(IntegrationTestDataFixture.DEPT_TEST_001_ID, inProgress.getDepartmentId(),
                "Encounter phải gắn đúng Khoa phòng");

        // Expected: Chỉ có duy nhất 1 encounter trong DB snapshot, không duplicate
        assertEquals(1, encounterDatabase.size(), "Không duplicate encounter");
        assertEquals(1, recordDatabase.size(), "Tạo đúng 1 bản draft record cho encounter");

        MedicalRecord record = recordDatabase.values().iterator().next();
        assertEquals(inProgress.getEncounterId(), record.getEncounterId());

        // Correlation ID được lưu vết
        assertEquals(correlationId, CorrelationContext.getCorrelationId());
    }
}
