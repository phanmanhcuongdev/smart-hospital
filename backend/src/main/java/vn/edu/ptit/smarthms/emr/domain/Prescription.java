package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(schema = "emr", name = "prescription")
public class Prescription {
    public enum Status { DRAFT, ORDERED, CANCELLED }

    @Id @Column(name = "prescription_id") private UUID prescriptionId;
    @Column(name = "encounter_id", nullable = false) private UUID encounterId;
    @Column(name = "prescriber_id", nullable = false) private UUID prescriberId;
    @Enumerated(EnumType.STRING) @Column(name = "prescription_status", nullable = false, length = 16)
    private Status status;
    @Column(name = "issued_at") private OffsetDateTime issuedAt;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "created_by", nullable = false, length = 80) private String createdBy;
    @Column(name = "updated_by", nullable = false, length = 80) private String updatedBy;

    protected Prescription() {}

    public static Prescription createDraft(UUID encounterId, UUID prescriberId, String actor) {
        Prescription p = new Prescription();
        OffsetDateTime now = OffsetDateTime.now();
        p.prescriptionId = UUID.randomUUID();
        p.encounterId = encounterId;
        p.prescriberId = prescriberId;
        p.status = Status.DRAFT;
        p.createdAt = now;
        p.updatedAt = now;
        p.createdBy = actor;
        p.updatedBy = actor;
        return p;
    }

    public void issue(String actor) {
        OffsetDateTime now = OffsetDateTime.now();
        this.status = Status.ORDERED;
        this.issuedAt = now;
        this.updatedAt = now;
        this.updatedBy = actor;
    }

    public void cancel(String actor) {
        this.status = Status.CANCELLED;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actor;
    }

    public boolean canCancel() {
        return Set.of(Status.DRAFT, Status.ORDERED).contains(status);
    }

    public UUID getPrescriptionId() { return prescriptionId; }
    public UUID getEncounterId() { return encounterId; }
    public UUID getPrescriberId() { return prescriberId; }
    public Status getStatus() { return status; }
    public OffsetDateTime getIssuedAt() { return issuedAt; }
}