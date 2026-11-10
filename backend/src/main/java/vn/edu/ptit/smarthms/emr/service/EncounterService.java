package vn.edu.ptit.smarthms.emr.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisStatus;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisType;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;
import vn.edu.ptit.smarthms.emr.port.AuditPort;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.repository.DiagnosisRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalRecordRepository;

@Service
public class EncounterService {
    private static final String DEV_ACTOR = "dev-stub"; // thay bằng user đăng nhập khi V.2 xong
    private static final String TARGET = "MEDICAL_ENCOUNTER";

    private final MedicalEncounterRepository repository;
    private final MedicalRecordRepository records;
    private final DiagnosisRepository diagnoses;
    private final ReferenceLookupPort lookup;
    private final AuditPort audit;

    public EncounterService(MedicalEncounterRepository repository, MedicalRecordRepository records,
                            DiagnosisRepository diagnoses, ReferenceLookupPort lookup, AuditPort audit) {
        this.repository = repository;
        this.records = records;
        this.diagnoses = diagnoses;
        this.lookup = lookup;
        this.audit = audit;
    }

    @Transactional
    public MedicalEncounter create(UUID patientId, UUID appointmentId, UUID attendingEmployeeId) {
        // IV7 M-01 / V-02: PID phải được xác minh.
        String identity = lookup.patientIdentityStatus(patientId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        if (!"VERIFIED".equals(identity)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient identity is not VERIFIED");
        }
        // IV7 M-20 / V-03: bác sĩ phụ trách phải đang làm việc (chứng chỉ hành nghề chờ OPEN-03).
        if (!lookup.practitionerExists(attendingEmployeeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attending employee not found");
        }
        if (!lookup.practitionerActive(attendingEmployeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Attending practitioner is not ACTIVE");
        }
        MedicalEncounter saved;
        try {
            saved = repository.saveAndFlush(
                    MedicalEncounter.create(patientId, appointmentId, attendingEmployeeId, DEV_ACTOR));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid appointment for this patient");
        }
        audit.success("ENCOUNTER_OPENED", TARGET, saved.getEncounterId(), null);
        return saved;
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
        EncounterStatus previous = e.getStatus();
        if (!previous.canTransitionTo(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change status from " + previous + " to " + next);
        }
        // IV7 M-03 / SM-01: chỉ sang DIAGNOSED khi đã có chẩn đoán chính được xác nhận.
        if (next == EncounterStatus.DIAGNOSED && !diagnoses.existsByEncounterIdAndDiagnosisTypeAndDiagnosisStatus(
                id, DiagnosisType.PRIMARY, DiagnosisStatus.CONFIRMED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A confirmed primary diagnosis is required before DIAGNOSED");
        }
        e.changeStatus(next, DEV_ACTOR, OffsetDateTime.now());
        if (next == EncounterStatus.IN_PROGRESS && records.findByEncounterId(id).isEmpty()) {
            records.save(MedicalRecord.createDraft(id, DEV_ACTOR));
        }
        if (previous == EncounterStatus.WAITING && next == EncounterStatus.IN_PROGRESS) {
            audit.success("ENCOUNTER_STARTED", TARGET, id, null);
        }
        return e;
    }

    @Transactional(readOnly = true)
    public MedicalRecord getRecord(UUID encounterId) {
        return records.findByEncounterId(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical record not found"));
    }
}