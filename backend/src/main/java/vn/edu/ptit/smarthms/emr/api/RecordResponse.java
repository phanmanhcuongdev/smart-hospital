package vn.edu.ptit.smarthms.emr.api;

import java.time.OffsetDateTime;
import java.util.UUID;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;

public record RecordResponse(UUID recordId, UUID encounterId, String recordStatus, int currentVersion,
                             int version, OffsetDateTime lockedAt, OffsetDateTime createdAt) {
    public static RecordResponse from(MedicalRecord r) {
        return new RecordResponse(r.getRecordId(), r.getEncounterId(), r.getRecordStatus().name(),
                r.getCurrentVersion(), r.getRowVersion(), r.getLockedAt(), r.getCreatedAt());
    }
}