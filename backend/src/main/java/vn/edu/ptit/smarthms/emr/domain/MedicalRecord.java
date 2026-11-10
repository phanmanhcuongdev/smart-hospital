package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "emr", name = "medical_record")
public class MedicalRecord {
    @Id @Column(name = "record_id") private UUID recordId;
    @Column(name = "encounter_id", nullable = false) private UUID encounterId;
    @Enumerated(EnumType.STRING) @Column(name = "record_status", nullable = false, length = 22)
    private RecordStatus recordStatus;
    @Column(name = "current_version", nullable = false) private int currentVersion;
    @Column(name = "locked_at") private OffsetDateTime lockedAt;
    @Column(name = "locked_by") private UUID lockedBy;
    @Column(name = "row_version", nullable = false) private int rowVersion;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "created_by", nullable = false, length = 80) private String createdBy;
    @Column(name = "updated_by", nullable = false, length = 80) private String updatedBy;

    protected MedicalRecord() {}

    public static MedicalRecord createDraft(UUID encounterId, String actor) {
        MedicalRecord r = new MedicalRecord();
        OffsetDateTime now = OffsetDateTime.now();
        r.recordId = UUID.randomUUID();
        r.encounterId = encounterId;
        r.recordStatus = RecordStatus.DRAFT;
        r.currentVersion = 1;
        r.rowVersion = 1;
        r.createdAt = now;
        r.updatedAt = now;
        r.createdBy = actor;
        r.updatedBy = actor;
        return r;
    }

        public void advanceVersion(String actor) {
        this.currentVersion++;
        this.rowVersion++;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actor;
    }

    public void submitForSignature(String actor) {
        this.recordStatus = RecordStatus.PENDING_SIGNATURE;
        this.rowVersion++;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actor;
    }

    public void lock(UUID userId, String actor) {
        OffsetDateTime now = OffsetDateTime.now();
        this.recordStatus = RecordStatus.LOCKED;
        this.lockedAt = now;
        this.lockedBy = userId;
        this.rowVersion++;
        this.updatedAt = now;
        this.updatedBy = actor;
    }

        public void returnToDraft(String actor) {
        this.recordStatus = RecordStatus.DRAFT;
        this.rowVersion++;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actor;
    }

    public int getRowVersion() { return rowVersion; }

    public UUID getLockedBy() { return lockedBy; }
    public UUID getRecordId() { return recordId; }
    public UUID getEncounterId() { return encounterId; }
    public RecordStatus getRecordStatus() { return recordStatus; }
    public int getCurrentVersion() { return currentVersion; }
    public OffsetDateTime getLockedAt() { return lockedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}