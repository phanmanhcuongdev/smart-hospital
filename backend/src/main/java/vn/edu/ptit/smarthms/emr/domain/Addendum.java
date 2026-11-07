package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** A correction linked to a LOCKED record. The original record is kept. */
/** Maps to emr.addendum (FR-EMR-04). */
@Entity
@Table(schema = "emr", name = "addendum")
public class Addendum extends AuditedEntity {

    public enum ConfirmationStatus { DRAFT, CONFIRMED }

    @Id
    @Column(name = "addendum_id", nullable = false)
    private UUID id;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "confirmation_status", nullable = false, length = 16)
    private ConfirmationStatus confirmationStatus;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    protected Addendum() {
    }

    public Addendum(UUID recordId, String reason, String content) {
        this.id = UUID.randomUUID();
        this.recordId = recordId;
        this.reason = reason;
        this.content = content;
        this.confirmationStatus = ConfirmationStatus.DRAFT;
    }

    public UUID getId() { return id; }
    public UUID getRecordId() { return recordId; }
    public String getReason() { return reason; }
    public String getContent() { return content; }
    public ConfirmationStatus getConfirmationStatus() { return confirmationStatus; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public UUID getConfirmedBy() { return confirmedBy; }
    public void setConfirmationStatus(ConfirmationStatus confirmationStatus) { this.confirmationStatus = confirmationStatus; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public void setConfirmedBy(UUID confirmedBy) { this.confirmedBy = confirmedBy; }
}
