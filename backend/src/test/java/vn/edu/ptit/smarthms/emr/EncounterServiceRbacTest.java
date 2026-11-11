package vn.edu.ptit.smarthms.emr;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.edu.ptit.smarthms.common.error.ForbiddenException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalRecordRepository;
import vn.edu.ptit.smarthms.emr.service.EncounterService;

class EncounterServiceRbacTest {

    private static final Actor DOCTOR = new Actor("EMP-DEMO-001", Set.of("DOCTOR"));
    private static final Actor NURSE = new Actor("demo-nurse", Set.of("NURSE"));
    private static final Actor ADMIN = new Actor("demo-admin", Set.of("ADMIN"));
    private static final Actor HR = new Actor("demo-hr", Set.of("HR_SPECIALIST"));

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> intf) {
        return (T) Proxy.newProxyInstance(intf.getClassLoader(), new Class<?>[] { intf },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("saveAndFlush") || name.equals("save")) {
                        return args[0];
                    }
                    if (name.equals("patientExists") || name.equals("practitionerExists")
                            || name.equals("departmentExists") || name.equals("isDepartmentActive")) {
                        return true;
                    }
                    if (name.equals("getPractitionerDetails")) {
                        UUID doctorId = (UUID) args[0];
                        return Optional.of(new ReferenceLookupPort.PractitionerDetails(
                                doctorId, "EMP-DOC", "Dr. Demo", "ACTIVE", true, true, UUID.randomUUID(), "Khoa Khám bệnh"));
                    }
                    if (name.equals("findById")) {
                        return Optional
                                .of(MedicalEncounter.create(UUID.randomUUID(), null, UUID.randomUUID(), "creator"));
                    }
                    if (name.equals("findByEncounterId")) {
                        return Optional.empty();
                    }
                    return null;
                });
    }

    private final MedicalEncounterRepository repository = stub(MedicalEncounterRepository.class);
    private final MedicalRecordRepository records = stub(MedicalRecordRepository.class);
    private final ReferenceLookupPort lookup = stub(ReferenceLookupPort.class);
    private final EncounterService service = new EncounterService(repository, records, lookup);

    @Test
    void doctorCanCreateEncounter() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        MedicalEncounter created = service.create(DOCTOR, patientId, null, doctorId);

        assertNotNull(created);
        assertEquals(patientId, created.getPatientId());
        assertEquals(doctorId, created.getAttendingEmployeeId());
        assertEquals("EMP-DEMO-001", created.getCreatedBy());
    }

    @Test
    void nurseCanCreateEncounter() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        MedicalEncounter created = service.create(NURSE, patientId, null, doctorId);

        assertNotNull(created);
        assertEquals("demo-nurse", created.getCreatedBy());
    }

    @Test
    void adminCanCreateEncounter() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        MedicalEncounter created = service.create(ADMIN, patientId, null, doctorId);

        assertNotNull(created);
        assertEquals("demo-admin", created.getCreatedBy());
    }

    @Test
    void hrUserCannotCreateEncounter() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        assertThrows(ForbiddenException.class, () -> service.create(HR, patientId, null, doctorId));
    }

    @Test
    void doctorCanChangeStatus() {
        UUID encounterId = UUID.randomUUID();

        MedicalEncounter updated = service.changeStatus(DOCTOR, encounterId, EncounterStatus.IN_PROGRESS);

        assertEquals(EncounterStatus.IN_PROGRESS, updated.getStatus());
        assertEquals("EMP-DEMO-001", updated.getUpdatedBy());
    }

    @Test
    void hrUserCannotChangeStatus() {
        UUID encounterId = UUID.randomUUID();

        assertThrows(ForbiddenException.class,
                () -> service.changeStatus(HR, encounterId, EncounterStatus.IN_PROGRESS));
    }
}
