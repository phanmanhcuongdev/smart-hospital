package vn.edu.ptit.smarthms.emr.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalRecordRepository;

@Service
public class EncounterService {
    private final MedicalEncounterRepository repository;
    private final MedicalRecordRepository records;
    private final ReferenceLookupPort lookup;

    public EncounterService(MedicalEncounterRepository repository,
                            MedicalRecordRepository records, ReferenceLookupPort lookup) {
        this.repository = repository;
        this.records = records;
        this.lookup = lookup;
    }

    @Transactional
    public MedicalEncounter create(Actor actor, UUID patientId, UUID appointmentId,
                                  UUID attendingEmployeeId, UUID departmentId) {
        actor.requireAnyRole("DOCTOR", "NURSE", "RECEPTION", "ADMIN");
        if (!lookup.patientExists(patientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }

        var practitionerOpt = lookup.getPractitionerDetails(attendingEmployeeId);
        if (practitionerOpt.isEmpty()) {
            if (!lookup.practitionerExists(attendingEmployeeId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attending employee not found");
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attending employee details not found");
        }

        var practitioner = practitionerOpt.get();
        if (!practitioner.active()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Attending practitioner is not in ACTIVE status (current: " + practitioner.status() + ")");
        }
        if (!practitioner.hasVerifiedLicense()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Attending practitioner does not hold a valid verified medical practice license (CCHN)");
        }

        UUID resolvedDepartmentId = departmentId != null ? departmentId : practitioner.departmentId();
        if (resolvedDepartmentId != null) {
            if (!lookup.departmentExists(resolvedDepartmentId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found");
            }
            if (!lookup.isDepartmentActive(resolvedDepartmentId)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Department is inactive");
            }
        }

        try {
            return repository.saveAndFlush(
                    MedicalEncounter.create(patientId, appointmentId, attendingEmployeeId, resolvedDepartmentId, actor.principalName()));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid appointment or department for this patient");
        }
    }

    @Transactional
    public MedicalEncounter create(Actor actor, UUID patientId, UUID appointmentId, UUID attendingEmployeeId) {
        return create(actor, patientId, appointmentId, attendingEmployeeId, null);
    }

    @Transactional(readOnly = true)
    public MedicalEncounter get(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
    }

    @Transactional
    public MedicalEncounter changeStatus(Actor actor, UUID id, EncounterStatus next,
                                         UUID newAttendingEmployeeId, UUID newDepartmentId) {
        actor.requireAnyRole("DOCTOR", "NURSE", "ADMIN");
        MedicalEncounter e = repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
        if (!e.getStatus().canTransitionTo(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change status from " + e.getStatus() + " to " + next);
        }

        UUID attendingId = newAttendingEmployeeId != null ? newAttendingEmployeeId : e.getAttendingEmployeeId();

        // Ràng buộc nghiệp vụ: Bác sĩ phụ trách phải ACTIVE và có chứng chỉ CCHN hợp lệ khi tiếp nhận ca khám
        if (next == EncounterStatus.IN_PROGRESS || newAttendingEmployeeId != null) {
            var practitionerOpt = lookup.getPractitionerDetails(attendingId);
            if (practitionerOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attending employee not found");
            }
            var practitioner = practitionerOpt.get();
            if (!practitioner.active()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Attending practitioner is not in ACTIVE status (current: " + practitioner.status() + ")");
            }
            if (!practitioner.hasVerifiedLicense()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Attending practitioner does not hold a valid verified medical practice license (CCHN)");
            }
        }

        UUID deptId = newDepartmentId != null ? newDepartmentId : e.getDepartmentId();
        if (deptId != null && (next == EncounterStatus.IN_PROGRESS || newDepartmentId != null)) {
            if (!lookup.departmentExists(deptId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found");
            }
            if (!lookup.isDepartmentActive(deptId)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Department is inactive");
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (newAttendingEmployeeId != null) {
            e.assignPractitioner(newAttendingEmployeeId, actor.principalName(), now);
        }
        if (newDepartmentId != null) {
            e.assignDepartment(newDepartmentId, actor.principalName(), now);
        }
        e.changeStatus(next, actor.principalName(), now);
        if (next == EncounterStatus.IN_PROGRESS && records.findByEncounterId(id).isEmpty()) {
            records.save(MedicalRecord.createDraft(id, actor.principalName()));
        }
        return e;
    }

    @Transactional
    public MedicalEncounter changeStatus(Actor actor, UUID id, EncounterStatus next) {
        return changeStatus(actor, id, next, null, null);
    }

    @Transactional(readOnly = true)
    public MedicalRecord getRecord(UUID encounterId) {
        return records.findByEncounterId(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical record not found"));
    }
}