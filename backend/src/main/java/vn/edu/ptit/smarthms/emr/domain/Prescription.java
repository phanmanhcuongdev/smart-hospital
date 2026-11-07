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

/** A prescription for one encounter. Prescribed is not dispensed (FR-EMR-03). */
@Entity
@Table(schema = "emr", name = "prescription")
public class Prescription extends AuditedEntity {

    public enum Status { DRAFT, ORDERED, CANCELLED }

    @Id
    @Column(name = "prescription_id", nullable = false)
    private UUID id;

    @Column(name = "encounter_id", nullable = false)
    private UUID encounterId;

    @Column(name = "prescriber_id", nullable = false)
    private UUID prescriberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "prescription_status", nullable = false, length = 16)
    private Status status;

    @Column(name = "issued_at")
    private Instant issuedAt;

    protected Prescription() {
    }

    public Prescription(UUID encounterId, UUID prescriberId) {
        this.id = UUID.randomUUID();
        this.encounterId = encounterId;
        this.prescriberId = prescriberId;
        this.status = Status.DRAFT;
    }

    public UUID getId() { return id; }
    public UUID getEncounterId() { return encounterId; }
    public UUID getPrescriberId() { return prescriberId; }
    public Status getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setStatus(Status status) { this.status = status; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
}
