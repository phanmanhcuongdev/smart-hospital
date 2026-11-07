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

/** A clinical laboratory / imaging order. Maps to emr.medical_order (FR-EMR-02). */
@Entity
@Table(schema = "emr", name = "medical_order")
public class MedicalOrder extends AuditedEntity {

    public enum Type { LAB, IMAGING, OTHER }
    public enum Status { DRAFT, ORDERED, ACKNOWLEDGED, IN_PROGRESS, COMPLETED, CANCELLED }

    @Id
    @Column(name = "order_id", nullable = false)
    private UUID id;

    @Column(name = "encounter_id", nullable = false)
    private UUID encounterId;

    @Column(name = "ordered_by", nullable = false)
    private UUID orderedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 16)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 18)
    private Status status;

    @Column(name = "external_order_ref", length = 160)
    private String externalOrderRef;

    @Column(name = "ordered_at")
    private Instant orderedAt;

    protected MedicalOrder() {
    }

    public MedicalOrder(UUID encounterId, UUID orderedBy, Type type, String externalOrderRef) {
        this.id = UUID.randomUUID();
        this.encounterId = encounterId;
        this.orderedBy = orderedBy;
        this.type = type;
        this.status = Status.DRAFT;
        this.externalOrderRef = externalOrderRef;
    }

    public UUID getId() { return id; }
    public UUID getEncounterId() { return encounterId; }
    public UUID getOrderedBy() { return orderedBy; }
    public Type getType() { return type; }
    public Status getStatus() { return status; }
    public String getExternalOrderRef() { return externalOrderRef; }
    public Instant getOrderedAt() { return orderedAt; }
    public void setStatus(Status status) { this.status = status; }
    public void setOrderedAt(Instant orderedAt) { this.orderedAt = orderedAt; }
}
