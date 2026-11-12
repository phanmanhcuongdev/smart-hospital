package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(schema = "emr", name = "medical_order")
public class MedicalOrder {
    public enum Type { LAB, IMAGING, OTHER }

    public enum Status {
        DRAFT, ORDERED, ACKNOWLEDGED, IN_PROGRESS, COMPLETED, CANCELLED;

        /** Giả định, cần đối chiếu State Machine trong IV.7. */
        public Set<Status> allowedNext() {
            return switch (this) {
                case DRAFT -> Set.of(ORDERED, CANCELLED);
                case ORDERED -> Set.of(ACKNOWLEDGED, CANCELLED);
                case ACKNOWLEDGED -> Set.of(IN_PROGRESS, CANCELLED);
                case IN_PROGRESS -> Set.of(COMPLETED);
                case COMPLETED, CANCELLED -> Set.of();
            };
        }
    }

    @Id @Column(name = "order_id") private UUID orderId;
    @Column(name = "encounter_id", nullable = false) private UUID encounterId;
    @Column(name = "ordered_by", nullable = false) private UUID orderedBy;
    @Enumerated(EnumType.STRING) @Column(name = "order_type", nullable = false, length = 16)
    private Type orderType;
    @Enumerated(EnumType.STRING) @Column(name = "order_status", nullable = false, length = 18)
    private Status orderStatus;
    @Column(name = "external_order_ref", length = 160) private String externalOrderRef;
    @Column(name = "ordered_at") private OffsetDateTime orderedAt;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "created_by", nullable = false, length = 80) private String createdBy;
    @Column(name = "updated_by", nullable = false, length = 80) private String updatedBy;

    protected MedicalOrder() {}

    public static MedicalOrder create(UUID encounterId, UUID orderedBy, Type type,
                                      String externalOrderRef, String actor) {
        MedicalOrder o = new MedicalOrder();
        OffsetDateTime now = OffsetDateTime.now();
        o.orderId = UUID.randomUUID();
        o.encounterId = encounterId;
        o.orderedBy = orderedBy;
        o.orderType = type;
        o.orderStatus = Status.ORDERED;
        o.externalOrderRef = externalOrderRef;
        o.orderedAt = now;
        o.createdAt = now;
        o.updatedAt = now;
        o.createdBy = actor;
        o.updatedBy = actor;
        return o;
    }

    public void changeStatus(Status next, String actor) {
        this.orderStatus = next;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actor;
    }

    public UUID getOrderId() { return orderId; }
    public UUID getEncounterId() { return encounterId; }
    public UUID getOrderedBy() { return orderedBy; }
    public Type getOrderType() { return orderType; }
    public Status getOrderStatus() { return orderStatus; }
    public String getExternalOrderRef() { return externalOrderRef; }
    public OffsetDateTime getOrderedAt() { return orderedAt; }
}