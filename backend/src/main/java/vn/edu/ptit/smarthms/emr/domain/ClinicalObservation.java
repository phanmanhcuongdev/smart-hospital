package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "emr", name = "clinical_observation")
public class ClinicalObservation {
    public enum SourceType { MANUAL, FIXTURE, EXTERNAL }
    public enum Quality { VALID, INVALID, UNKNOWN }

    @Id @Column(name = "observation_id") private UUID observationId;
    @Column(name = "encounter_id", nullable = false) private UUID encounterId;
    @Column(name = "metric_code", nullable = false, length = 60) private String metricCode;
    @Column(name = "numeric_value", precision = 14, scale = 4) private BigDecimal numericValue;
    @Column(name = "text_value", columnDefinition = "text") private String textValue;
    @Column(name = "unit_code", length = 40) private String unitCode;
    @Column(name = "observed_at", nullable = false) private OffsetDateTime observedAt;
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;
    @Column(name = "source_ref", nullable = false, length = 160) private String sourceRef;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Quality quality;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "created_by", nullable = false, length = 80) private String createdBy;
    @Column(name = "updated_by", nullable = false, length = 80) private String updatedBy;

    protected ClinicalObservation() {}

    public static ClinicalObservation create(UUID encounterId, String metricCode,
            BigDecimal numericValue, String textValue, String unitCode, OffsetDateTime observedAt,
            SourceType sourceType, String sourceRef, Quality quality, String actor) {
        ClinicalObservation o = new ClinicalObservation();
        OffsetDateTime now = OffsetDateTime.now();
        o.observationId = UUID.randomUUID();
        o.encounterId = encounterId;
        o.metricCode = metricCode;
        o.numericValue = numericValue;
        o.textValue = textValue;
        o.unitCode = unitCode;
        o.observedAt = observedAt;
        o.sourceType = sourceType;
        o.sourceRef = sourceRef;
        o.quality = quality;
        o.createdAt = now;
        o.updatedAt = now;
        o.createdBy = actor;
        o.updatedBy = actor;
        return o;
    }

    public UUID getObservationId() { return observationId; }
    public UUID getEncounterId() { return encounterId; }
    public String getMetricCode() { return metricCode; }
    public BigDecimal getNumericValue() { return numericValue; }
    public String getTextValue() { return textValue; }
    public String getUnitCode() { return unitCode; }
    public OffsetDateTime getObservedAt() { return observedAt; }
    public SourceType getSourceType() { return sourceType; }
    public String getSourceRef() { return sourceRef; }
    public Quality getQuality() { return quality; }
}