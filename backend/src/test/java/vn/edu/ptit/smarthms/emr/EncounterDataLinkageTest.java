package vn.edu.ptit.smarthms.emr;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalRecordRepository;
import vn.edu.ptit.smarthms.emr.service.EncounterService;

class EncounterDataLinkageTest {

    private static final Actor DOCTOR = new Actor("EMP-DEMO-001", Set.of("DOCTOR"));

    private MedicalEncounterRepository repository;
    private MedicalRecordRepository records;
    private ReferenceLookupPort lookup;
    private EncounterService service;

    private UUID patientId;
    private UUID doctorId;
    private UUID primaryDeptId;
    private UUID otherDeptId;

    // Test flags
    private boolean patientFound;
    private boolean practitionerExists;
    private String practitionerStatus;
    private boolean practitionerHasLicense;
    private boolean primaryDeptExists;
    private boolean primaryDeptActive;
    private boolean otherDeptExists;
    private boolean otherDeptActive;

    @BeforeEach
    void setup() {
        patientId = UUID.randomUUID();
        doctorId = UUID.randomUUID();
        primaryDeptId = UUID.randomUUID();
        otherDeptId = UUID.randomUUID();

        patientFound = true;
        practitionerExists = true;
        practitionerStatus = "ACTIVE";
        practitionerHasLicense = true;
        primaryDeptExists = true;
        primaryDeptActive = true;
        otherDeptExists = true;
        otherDeptActive = true;

        repository = (MedicalEncounterRepository) Proxy.newProxyInstance(
                MedicalEncounterRepository.class.getClassLoader(),
                new Class<?>[]{MedicalEncounterRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("saveAndFlush") || method.getName().equals("save")) {
                        return args[0];
                    }
                    if (method.getName().equals("findById")) {
                        return Optional.of(MedicalEncounter.create(patientId, null, doctorId, primaryDeptId, "creator"));
                    }
                    return null;
                });

        records = (MedicalRecordRepository) Proxy.newProxyInstance(
                MedicalRecordRepository.class.getClassLoader(),
                new Class<?>[]{MedicalRecordRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByEncounterId")) {
                        return Optional.empty();
                    }
                    if (method.getName().equals("save")) {
                        return args[0];
                    }
                    return null;
                });

        lookup = (ReferenceLookupPort) Proxy.newProxyInstance(
                ReferenceLookupPort.class.getClassLoader(),
                new Class<?>[]{ReferenceLookupPort.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("patientExists")) {
                        return patientFound;
                    }
                    if (name.equals("practitionerExists")) {
                        return practitionerExists;
                    }
                    if (name.equals("getPractitionerDetails")) {
                        if (!practitionerExists) {
                            return Optional.empty();
                        }
                        boolean active = "ACTIVE".equalsIgnoreCase(practitionerStatus);
                        return Optional.of(new ReferenceLookupPort.PractitionerDetails(
                                doctorId, "DOC-001", "Dr. Test Clinician",
                                practitionerStatus, active, practitionerHasLicense,
                                primaryDeptId, "Khoa Ngoại Tổng Hợp"
                        ));
                    }
                    if (name.equals("departmentExists")) {
                        UUID dId = (UUID) args[0];
                        if (dId.equals(primaryDeptId)) return primaryDeptExists;
                        if (dId.equals(otherDeptId)) return otherDeptExists;
                        return false;
                    }
                    if (name.equals("isDepartmentActive")) {
                        UUID dId = (UUID) args[0];
                        if (dId.equals(primaryDeptId)) return primaryDeptActive;
                        if (dId.equals(otherDeptId)) return otherDeptActive;
                        return false;
                    }
                    return null;
                });

        service = new EncounterService(repository, records, lookup);
    }

    @Test
    void rejectsWhenDoctorDoesNotExist() {
        practitionerExists = false;

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.create(DOCTOR, patientId, null, doctorId, null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Attending employee"));
    }

    @Test
    void rejectsWhenDoctorIsSuspended() {
        practitionerStatus = "SUSPENDED";

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.create(DOCTOR, patientId, null, doctorId, null));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("ACTIVE"));
    }

    @Test
    void rejectsWhenDoctorIsTerminated() {
        practitionerStatus = "TERMINATED";

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.create(DOCTOR, patientId, null, doctorId, null));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("ACTIVE"));
    }

    @Test
    void rejectsWhenDoctorIsOnboarding() {
        practitionerStatus = "ONBOARDING";

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.create(DOCTOR, patientId, null, doctorId, null));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("ACTIVE"));
    }

    @Test
    void rejectsWhenDoctorLacksVerifiedLicense() {
        practitionerHasLicense = false;

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.create(DOCTOR, patientId, null, doctorId, null));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("license") || ex.getReason().contains("CCHN"));
    }

    @Test
    void rejectsWhenExplicitDepartmentDoesNotExist() {
        otherDeptExists = false;

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.create(DOCTOR, patientId, null, doctorId, otherDeptId));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Department not found"));
    }

    @Test
    void rejectsWhenExplicitDepartmentIsInactive() {
        otherDeptActive = false;

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.create(DOCTOR, patientId, null, doctorId, otherDeptId));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("inactive"));
    }

    @Test
    void createsEncounterWithExplicitDepartment() {
        MedicalEncounter encounter = service.create(DOCTOR, patientId, null, doctorId, otherDeptId);

        assertNotNull(encounter);
        assertEquals(patientId, encounter.getPatientId());
        assertEquals(doctorId, encounter.getAttendingEmployeeId());
        assertEquals(otherDeptId, encounter.getDepartmentId());
    }

    @Test
    void createsEncounterFallingBackToDoctorPrimaryDepartment() {
        // When departmentId is null, should automatically link doctor's primary department
        MedicalEncounter encounter = service.create(DOCTOR, patientId, null, doctorId, null);

        assertNotNull(encounter);
        assertEquals(patientId, encounter.getPatientId());
        assertEquals(doctorId, encounter.getAttendingEmployeeId());
        assertEquals(primaryDeptId, encounter.getDepartmentId());
    }

    @Test
    void rejectsReceivingEncounterWhenDoctorIsSuspended() {
        practitionerStatus = "SUSPENDED";

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.changeStatus(DOCTOR, UUID.randomUUID(), vn.edu.ptit.smarthms.emr.domain.EncounterStatus.IN_PROGRESS));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("ACTIVE"));
    }

    @Test
    void rejectsReceivingEncounterWhenDoctorLacksVerifiedLicense() {
        practitionerHasLicense = false;

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                service.changeStatus(DOCTOR, UUID.randomUUID(), vn.edu.ptit.smarthms.emr.domain.EncounterStatus.IN_PROGRESS));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertTrue(ex.getReason().contains("license") || ex.getReason().contains("CCHN"));
    }

    @Test
    void receivesEncounterSuccessfullyWhenDoctorAndDepartmentValid() {
        MedicalEncounter encounter = service.changeStatus(
                DOCTOR, UUID.randomUUID(), vn.edu.ptit.smarthms.emr.domain.EncounterStatus.IN_PROGRESS);

        assertNotNull(encounter);
        assertEquals(vn.edu.ptit.smarthms.emr.domain.EncounterStatus.IN_PROGRESS, encounter.getStatus());
        assertNotNull(encounter.getStartedAt());
    }

    @Test
    void receivesEncounterWithReassignedDoctorAndDepartment() {
        UUID newDoctorId = UUID.randomUUID();
        MedicalEncounter encounter = service.changeStatus(
                DOCTOR, UUID.randomUUID(), vn.edu.ptit.smarthms.emr.domain.EncounterStatus.IN_PROGRESS,
                newDoctorId, otherDeptId);

        assertNotNull(encounter);
        assertEquals(vn.edu.ptit.smarthms.emr.domain.EncounterStatus.IN_PROGRESS, encounter.getStatus());
        assertEquals(newDoctorId, encounter.getAttendingEmployeeId());
        assertEquals(otherDeptId, encounter.getDepartmentId());
    }
}
