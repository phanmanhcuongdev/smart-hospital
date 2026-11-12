package vn.edu.ptit.smarthms.emr.service;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.domain.Addendum;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisStatus;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisType;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;
import vn.edu.ptit.smarthms.emr.domain.RecordStatus;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.emr.repository.AddendumRepository;
import vn.edu.ptit.smarthms.emr.repository.DiagnosisRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.repository.MedicalRecordRepository;
import vn.edu.ptit.smarthms.emr.repository.RecordSnapshotRepository;
import vn.edu.ptit.smarthms.emr.repository.RecordSnapshotRepository.VersionRow;

@Service
public class RecordService {
    private final MedicalRecordRepository records;
    private final MedicalEncounterRepository encounters;
    private final DiagnosisRepository diagnoses;
    private final AddendumRepository addenda;
    private final RecordSnapshotRepository snapshots;
    private final ReferenceLookupPort lookup;

    public RecordService(MedicalRecordRepository records, MedicalEncounterRepository encounters,
                         DiagnosisRepository diagnoses, AddendumRepository addenda,
                         RecordSnapshotRepository snapshots, ReferenceLookupPort lookup) {
        this.records = records;
        this.encounters = encounters;
        this.diagnoses = diagnoses;
        this.addenda = addenda;
        this.snapshots = snapshots;
        this.lookup = lookup;
    }

    /** Chốt một phiên bản hồ sơ đang soạn thảo. */
    @Transactional
    public VersionRow snapshot(Actor actor, UUID encounterId) {
        actor.requireAnyRole("DOCTOR", "ADMIN");
        MedicalRecord r = findRecord(encounterId);
        requireStatus(r, RecordStatus.DRAFT, "create a version");
        int versionNo = r.getCurrentVersion();
        snapshots.insertVersion(r.getRecordId(), versionNo, snapshots.buildPayload(encounterId), actor.principalName());
        r.advanceVersion(actor.principalName());
        return snapshots.get(r.getRecordId(), versionNo).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<VersionRow> listVersions(UUID encounterId) {
        return snapshots.list(findRecord(encounterId).getRecordId());
    }

    @Transactional(readOnly = true)
    public VersionRow getVersion(UUID encounterId, int versionNo) {
        return snapshots.get(findRecord(encounterId).getRecordId(), versionNo).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Record version not found"));
    }

    @Transactional
    public MedicalRecord submit(Actor actor, UUID encounterId) {
        actor.requireAnyRole("DOCTOR", "ADMIN");
        MedicalEncounter e = encounters.findById(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Encounter not found"));
        MedicalRecord r = findRecord(encounterId);
        requireStatus(r, RecordStatus.DRAFT, "submit the record");
        if (e.getStatus() != EncounterStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Encounter must be COMPLETED before the record is submitted");
        }
        if (!diagnoses.existsByEncounterIdAndDiagnosisTypeAndDiagnosisStatus(
                encounterId, DiagnosisType.PRIMARY, DiagnosisStatus.CONFIRMED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Record needs a confirmed primary diagnosis");
        }
        r.submitForSignature(actor.principalName());
        return r;
    }

    @Transactional
    public MedicalRecord lock(Actor actor, UUID encounterId, UUID userId) {
        actor.requireAnyRole("DOCTOR", "RECORDS_MANAGER", "ADMIN");
        MedicalRecord r = findRecord(encounterId);
        requireStatus(r, RecordStatus.PENDING_SIGNATURE, "lock the record");
        requireUser(userId);
        snapshots.insertVersion(r.getRecordId(), r.getCurrentVersion(),
                snapshots.buildPayload(encounterId), actor.principalName());
        r.lock(userId, actor.principalName());
        return r;
    }

    @Transactional
    public Addendum createAddendum(Actor actor, UUID encounterId, String reason, String content) {
        actor.requireAnyRole("DOCTOR", "ADMIN");
        MedicalRecord r = findRecord(encounterId);
        requireStatus(r, RecordStatus.LOCKED, "add an addendum");
        return addenda.save(Addendum.create(r.getRecordId(), reason.trim(), content.trim(), actor.principalName()));
    }

    @Transactional(readOnly = true)
    public List<Addendum> listAddenda(UUID encounterId) {
        return addenda.findByRecordIdOrderByCreatedAtAsc(findRecord(encounterId).getRecordId());
    }

    @Transactional
    public Addendum confirmAddendum(Actor actor, UUID encounterId, UUID addendumId, UUID userId) {
        actor.requireAnyRole("DOCTOR", "RECORDS_MANAGER", "ADMIN");
        MedicalRecord r = findRecord(encounterId);
        Addendum a = addenda.findById(addendumId)
                .filter(x -> x.getRecordId().equals(r.getRecordId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Addendum not found"));
        if (a.getConfirmationStatus() != Addendum.Status.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Addendum already confirmed");
        }
        requireUser(userId);
        a.confirm(userId, actor.principalName());
        return a;
    }

    private MedicalRecord findRecord(UUID encounterId) {
        return records.findByEncounterId(encounterId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical record not found"));
    }

    private void requireStatus(MedicalRecord r, RecordStatus expected, String action) {
        if (r.getRecordStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot " + action + " when record is " + r.getRecordStatus());
        }
    }

    private void requireUser(UUID userId) {
        if (!lookup.userExists(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }
}
