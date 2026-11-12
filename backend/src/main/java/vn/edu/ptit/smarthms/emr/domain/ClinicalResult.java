package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "emr", name = "clinical_result")
public class ClinicalResult {

    public enum Quality {
        VALID, QUARANTINED
    }

    @Id
    @Column(name = "result_id")
    private UUID resultId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "source_result_id", nullable = false, length = 160)
    private String sourceResultId;

    @Column(name = "source_version", nullable = false, length = 60)
    private String sourceVersion;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "result_at")
    private OffsetDateTime resultAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_payload", nullable = false)
    private String resultPayload;

    @Column(name = "abnormal_flag")
    private Boolean abnormalFlag;

    @Column(name = "quality", nullable = false, length = 16)
    private String quality;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 80)
    private String updatedBy;

    protected ClinicalResult() {}

    public static ClinicalResult create(UUID orderId, String sourceSystem, String sourceResultId,
                                        String sourceVersion, OffsetDateTime resultAt,
                                        String resultPayload, Boolean abnormalFlag, Quality quality,
                                        String actor) {
        ClinicalResult r = new ClinicalResult();
        OffsetDateTime now = OffsetDateTime.now();
        r.resultId = UUID.randomUUID();
        r.orderId = orderId;
        r.sourceSystem = sourceSystem;
        r.sourceResultId = sourceResultId;
        r.sourceVersion = sourceVersion;
        r.receivedAt = now;
        r.resultAt = resultAt != null ? resultAt : now;
        r.resultPayload = resultPayload;
        r.abnormalFlag = abnormalFlag;
        r.quality = quality.name();
        r.createdAt = now;
        r.updatedAt = now;
        r.createdBy = actor;
        r.updatedBy = actor;
        return r;
    }

    public UUID getResultId() { return resultId; }
    public UUID getOrderId() { return orderId; }
    public String getSourceSystem() { return sourceSystem; }
    public String getSourceResultId() { return sourceResultId; }
    public String getSourceVersion() { return sourceVersion; }
    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public OffsetDateTime getResultAt() { return resultAt; }
    public String getResultPayload() { return resultPayload; }
    public Boolean getAbnormalFlag() { return abnormalFlag; }
    public String getQuality() { return quality; }
    public boolean isQuarantined() { return Quality.QUARANTINED.name().equalsIgnoreCase(quality); }
}
