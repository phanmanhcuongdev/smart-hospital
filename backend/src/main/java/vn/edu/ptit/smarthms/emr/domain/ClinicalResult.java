package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** A result received for an order, reconciled by source tuple (FR-EMR-02, BR-100). */
@Entity
@Table(schema = "emr", name = "clinical_result")
public class ClinicalResult extends AuditedEntity {

    public enum Quality { VALID, QUARANTINED }

    @Id
    @Column(name = "result_id", nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "source_result_id", nullable = false, length = 160)
    private String sourceResultId;

    @Column(name = "source_version", nullable = false, length = 60)
    private String sourceVersion;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_payload", nullable = false, columnDefinition = "jsonb")
    private String resultPayload;

    @Column(name = "abnormal_flag")
    private Boolean abnormalFlag;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality", nullable = false, length = 16)
    private Quality quality;

    protected ClinicalResult() {
    }

    public ClinicalResult(UUID orderId, String sourceSystem, String sourceResultId,
            String sourceVersion, String resultPayload, Boolean abnormalFlag, Quality quality) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.sourceSystem = sourceSystem;
        this.sourceResultId = sourceResultId;
        this.sourceVersion = sourceVersion;
        this.receivedAt = Instant.now();
        this.resultPayload = resultPayload;
        this.abnormalFlag = abnormalFlag;
        this.quality = quality;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getSourceSystem() { return sourceSystem; }
    public String getSourceResultId() { return sourceResultId; }
    public String getSourceVersion() { return sourceVersion; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getResultPayload() { return resultPayload; }
    public Boolean getAbnormalFlag() { return abnormalFlag; }
    public Quality getQuality() { return quality; }
}
