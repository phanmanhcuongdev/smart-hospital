package vn.edu.ptit.smarthms.emr.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalRecordRepository;

@Service
public class EncounterService {
    private static final String DEV_ACTOR = "dev-stub"; // thay bằng user đăng nhập khi V.2 xong

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
    public MedicalEncounter create(UUID patientId, UUID appointmentId, UUID attendingEmployeeId) {
        if (!lookup.patientExists(patientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found");
        }
        if (!lookup.practitionerExists(attendingEmployeeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attending employee not found");
        }
        try {
            return repository.saveAndFlush(
                    MedicalEncounter.create(patientId, appointmentId, attendingEmployeeId, DEV_ACTOR));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid appointment for this patient");
        }
    }

    @Transactional(readOnly = true)
    public MedicalEncounter get(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
    }

    @Transactional
    public MedicalEncounter changeStatus(UUID id, EncounterStatus next) {
        MedicalEncounter e = repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
        if (!e.getStatus().canTransitionTo(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change status from " + e.getStatus() + " to " + next);
        }
        e.changeStatus(next, DEV_ACTOR, OffsetDateTime.now());
        if (next == EncounterStatus.IN_PROGRESS && records.findByEncounterId(id).isEmpty()) {
            records.save(MedicalRecord.createDraft(id, DEV_ACTOR));
        }
        return e;
    }

    @Transactional(readOnly = true)
    public MedicalRecord getRecord(UUID encounterId) {
        return records.findByEncounterId(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical record not found"));
    }
}