package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** One vital sign or other measured value. Maps to emr.clinical_observation (FR-EMR-01). */
@Entity
@Table(schema = "emr", name = "clinical_observation")
public class ClinicalObservation extends AuditedEntity {

    public enum SourceType { MANUAL, FIXTURE, EXTERNAL }
    public enum Quality { VALID, INVALID, UNKNOWN }

    @Id
    @Column(name = "observation_id", nullable = false)
    private UUID id;

    @Column(name = "encounter_id", nullable = false)
    private UUID encounterId;

    @Column(name = "metric_code", nullable = false, length = 60)
    private String metricCode;

    @Column(name = "numeric_value", precision = 14, scale = 4)
    private BigDecimal numericValue;

    @Column(name = "text_value")
    private String textValue;

    @Column(name = "unit_code", length = 40)
    private String unitCode;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Column(name = "source_ref", length = 160)
    private String sourceRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality", nullable = false, length = 16)
    private Quality quality;

    protected ClinicalObservation() {
    }

    public ClinicalObservation(UUID encounterId, String metricCode, BigDecimal numericValue,
            String textValue, String unitCode, Instant observedAt, SourceType sourceType) {
        this.id = UUID.randomUUID();
        this.encounterId = encounterId;
        this.metricCode = metricCode;
        this.numericValue = numericValue;
        this.textValue = textValue;
        this.unitCode = unitCode;
        this.observedAt = observedAt;
        this.sourceType = sourceType;
        this.quality = Quality.VALID;
    }

    public UUID getId() { return id; }
    public UUID getEncounterId() { return encounterId; }
    public String getMetricCode() { return metricCode; }
    public BigDecimal getNumericValue() { return numericValue; }
    public String getTextValue() { return textValue; }
    public String getUnitCode() { return unitCode; }
    public Instant getObservedAt() { return observedAt; }
    public SourceType getSourceType() { return sourceType; }
    public Quality getQuality() { return quality; }
    public void setQuality(Quality quality) { this.quality = quality; }
}
