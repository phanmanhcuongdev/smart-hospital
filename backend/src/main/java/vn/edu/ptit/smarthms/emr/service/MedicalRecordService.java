package vn.edu.ptit.smarthms.emr.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.error.ForbiddenException;
import vn.edu.ptit.smarthms.common.error.NotFoundException;
import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.AddendumRequest;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.LockRequest;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.AddendumResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.RecordResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.RecordVersionResponse;
import vn.edu.ptit.smarthms.emr.domain.Addendum;
import vn.edu.ptit.smarthms.emr.domain.AddendumRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecordRepository;
import vn.edu.ptit.smarthms.emr.domain.RecordVersion;
import vn.edu.ptit.smarthms.emr.domain.RecordVersionRepository;

/** Medical record lifecycle (FR-EMR-04, BR-039 to BR-046). Demo-lock only: no legal signature. */
@Service
@Transactional
public class MedicalRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(MedicalRecordService.class);

    private final MedicalEncounterRepository encounters;
    private final MedicalRecordRepository records;
    private final RecordVersionRepository versions;
    private final AddendumRepository addenda;
    private final PractitionerPort practitionerPort;

    public MedicalRecordService(MedicalEncounterRepository encounters,
            MedicalRecordRepository records, RecordVersionRepository versions,
            AddendumRepository addenda, PractitionerPort practitionerPort) {
        this.encounters = encounters;
        this.records = records;
        this.versions = versions;
        this.addenda = addenda;
        this.practitionerPort = practitionerPort;
    }

    public RecordResponse getRecordByEncounter(Actor actor, UUID encounterId) {
        actor.requireAnyRole("DOCTOR", "NURSE", "CLS_TECHNICIAN", "RECORDS_MANAGER", "RECEPTION");
        MedicalEncounter encounter = requireEncounter(encounterId);
        requireScope(actor, encounter);
        MedicalRecord record = requireRecordByEncounter(encounterId);
        if (record.getStatus() == MedicalRecord.Status.LOCKED
                || record.getStatus() == MedicalRecord.Status.ARCHIVED) {
            LOG.info("Medical record {} accessed by {} (status {})", record.getId(),
                    actor.principalName(), record.getStatus());
        }
        return RecordResponse.from(record);
    }

    public RecordResponse submitForSignature(Actor actor, UUID recordId, LockRequest request) {
        actor.requireAnyRole("DOCTOR", "RECORDS_MANAGER");
        MedicalRecord record = requireRecord(recordId);
        requireCanChange(actor, record);
        if (record.getStatus() != MedicalRecord.Status.DRAFT) {
            throw new ConflictException("Only a DRAFT record can be submitted for signature.");
        }
        if (request.expectedRowVersion().equals(record.getRowVersion()) == false) {
            throw new ConflictException("The record changed since it was read; reload and try again.");
        }
        int versionNo = record.getCurrentVersion() + 1;
        String payload = "{\"recordId\":\"" + record.getId() + "\",\"encounterId\":\""
                + record.getEncounterId() + "\",\"status\":\"PENDING_SIGNATURE\",\"version\":"
                + versionNo + "}";
        RecordVersion version = new RecordVersion(record.getId(), versionNo, payload, sha256Hex(payload));
        version.setCreatedBy(actor.principalName());
        version.setUpdatedBy(actor.principalName());
        versions.save(version);
        record.setStatus(MedicalRecord.Status.PENDING_SIGNATURE);
        record.setCurrentVersion(versionNo);
        record.setUpdatedBy(actor.principalName());
        return RecordResponse.from(records.save(record));
    }

    public RecordResponse lockRecord(Actor actor, UUID recordId) {
        actor.requireAnyRole("DOCTOR", "RECORDS_MANAGER");
        MedicalRecord record = requireRecord(recordId);
        requireCanChange(actor, record);
        if (record.getStatus() != MedicalRecord.Status.PENDING_SIGNATURE) {
            throw new ConflictException("Only a PENDING_SIGNATURE record can be locked.");
        }
        record.setStatus(MedicalRecord.Status.LOCKED);
        record.setLockedAt(Instant.now());
        record.setLockedBy(null);
        record.setUpdatedBy(actor.principalName());
        return RecordResponse.from(records.save(record));
    }

    public RecordResponse archiveRecord(Actor actor, UUID recordId) {
        actor.requireRole("RECORDS_MANAGER");
        MedicalRecord record = requireRecord(recordId);
        if (record.getStatus() != MedicalRecord.Status.LOCKED) {
            throw new ConflictException("Only a LOCKED record can be archived.");
        }
        record.setStatus(MedicalRecord.Status.ARCHIVED);
        record.setUpdatedBy(actor.principalName());
        return RecordResponse.from(records.save(record));
    }

    public AddendumResponse addAddendum(Actor actor, UUID recordId, AddendumRequest request) {
        actor.requireAnyRole("DOCTOR", "RECORDS_MANAGER");
        MedicalRecord record = requireRecord(recordId);
        requireCanChange(actor, record);
        if (record.getStatus() != MedicalRecord.Status.LOCKED) {
            throw new ConflictException("Addenda can only be added to a LOCKED record.");
        }
        Addendum addendum = new Addendum(recordId, request.reason(), request.content());
        addendum.setCreatedBy(actor.principalName());
        addendum.setUpdatedBy(actor.principalName());
        return AddendumResponse.from(addenda.save(addendum));
    }

    public AddendumResponse confirmAddendum(Actor actor, UUID addendumId) {
        actor.requireAnyRole("DOCTOR", "RECORDS_MANAGER");
        Addendum addendum = addenda.findById(addendumId)
                .orElseThrow(() -> new NotFoundException("Addendum not found: " + addendumId));
        MedicalRecord record = requireRecord(addendum.getRecordId());
        requireCanChange(actor, record);
        if (addendum.getConfirmationStatus() != Addendum.ConfirmationStatus.DRAFT) {
            throw new ConflictException("Addendum is already confirmed.");
        }
        if (record.getStatus() != MedicalRecord.Status.LOCKED) {
            throw new ConflictException("The parent record is no longer locked.");
        }
        UUID confirmerId = practitionerPort.employeeIdForCode(actor.principalName()).orElse(null);
        if (confirmerId == null) {
            throw new ValidationException("The confirming user must map to a registered employee.");
        }
        addendum.setConfirmationStatus(Addendum.ConfirmationStatus.CONFIRMED);
        addendum.setConfirmedAt(Instant.now());
        addendum.setConfirmedBy(confirmerId);
        addendum.setUpdatedBy(actor.principalName());
        return AddendumResponse.from(addenda.save(addendum));
    }

    public List<RecordVersionResponse> listVersions(Actor actor, UUID recordId) {
        actor.requireAnyRole("DOCTOR", "NURSE", "CLS_TECHNICIAN", "RECORDS_MANAGER", "RECEPTION");
        MedicalRecord record = requireRecord(recordId);
        List<RecordVersionResponse> result = new ArrayList<>();
        for (RecordVersion version : versions.findByIdRecordIdOrderByIdVersionNoAsc(record.getId())) {
            result.add(RecordVersionResponse.from(version));
        }
        return result;
    }

    public List<AddendumResponse> listAddenda(Actor actor, UUID recordId) {
        actor.requireAnyRole("DOCTOR", "NURSE", "CLS_TECHNICIAN", "RECORDS_MANAGER", "RECEPTION");
        MedicalRecord record = requireRecord(recordId);
        List<AddendumResponse> result = new ArrayList<>();
        for (Addendum addendum : addenda.findByRecordId(record.getId())) {
            result.add(AddendumResponse.from(addendum));
        }
        return result;
    }

    private MedicalRecord requireRecord(UUID recordId) {
        return records.findById(recordId)
                .orElseThrow(() -> new NotFoundException("Medical record not found: " + recordId));
    }

    private MedicalRecord requireRecordByEncounter(UUID encounterId) {
        return records.findByEncounterId(encounterId)
                .orElseThrow(() -> new NotFoundException("No medical record for encounter: " + encounterId));
    }

    private MedicalEncounter requireEncounter(UUID encounterId) {
        return encounters.findById(encounterId)
                .orElseThrow(() -> new NotFoundException("Encounter not found: " + encounterId));
    }

    private void requireCanChange(Actor actor, MedicalRecord record) {
        MedicalEncounter encounter = requireEncounter(record.getEncounterId());
        requireScope(actor, encounter);
    }

    private void requireScope(Actor actor, MedicalEncounter encounter) {
        if (actor.hasRole("DOCTOR")) {
            UUID doctorId = practitionerPort.employeeIdForCode(actor.principalName()).orElse(null);
            if (doctorId == null || doctorId.equals(encounter.getAttendingEmployeeId()) == false) {
                throw new ForbiddenException("Doctor may only change assigned encounters.");
            }
        }
    }

    private String sha256Hex(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
