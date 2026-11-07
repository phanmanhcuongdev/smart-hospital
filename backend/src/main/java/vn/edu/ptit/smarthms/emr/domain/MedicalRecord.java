package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** The clinical record of one encounter, with a demo lock (not a legal signature). */
/** Maps to emr.medical_record (FR-EMR-04). */
@Entity
@Table(schema = "emr", name = "medical_record")
public class MedicalRecord extends AuditedEntity {

    public enum Status { DRAFT, PENDING_SIGNATURE, LOCKED, ARCHIVED }

    @Id
    @Column(name = "record_id", nullable = false)
    private UUID id;

    @Column(name = "encounter_id", nullable = false, unique = true)
    private UUID encounterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = false, length = 20)
    private Status status;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;

    @Column(name = "locked_by")
    private UUID lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Integer rowVersion = 1;

    protected MedicalRecord() {
    }

    public MedicalRecord(UUID encounterId) {
        this.id = UUID.randomUUID();
        this.encounterId = encounterId;
        this.status = Status.DRAFT;
    }

    public UUID getId() { return id; }
    public UUID getEncounterId() { return encounterId; }
    public Status getStatus() { return status; }
    public Integer getCurrentVersion() { return currentVersion; }
    public UUID getLockedBy() { return lockedBy; }
    public Instant getLockedAt() { return lockedAt; }
    public Integer getRowVersion() { return rowVersion; }
    public void setStatus(Status status) { this.status = status; }
    public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }
    public void setLockedBy(UUID lockedBy) { this.lockedBy = lockedBy; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }
}
