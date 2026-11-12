package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(schema = "emr", name = "prescription_item")
public class PrescriptionItem {

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "prescription_id") private UUID prescriptionId;
        @Column(name = "line_no") private int lineNo;

        protected Key() {}

        public Key(UUID prescriptionId, int lineNo) {
            this.prescriptionId = prescriptionId;
            this.lineNo = lineNo;
        }

        public UUID getPrescriptionId() { return prescriptionId; }
        public int getLineNo() { return lineNo; }

        @Override public boolean equals(Object o) {
            return o instanceof Key k && lineNo == k.lineNo && Objects.equals(prescriptionId, k.prescriptionId);
        }

        @Override public int hashCode() { return Objects.hash(prescriptionId, lineNo); }
    }

    @EmbeddedId private Key id;
    @Column(name = "medication_id", nullable = false) private UUID medicationId;
    @Column(name = "medication_label", nullable = false, length = 240) private String medicationLabel;
    @Column(nullable = false, length = 100) private String dose;
    @Column(nullable = false, length = 80) private String route;
    @Column(nullable = false, length = 120) private String frequency;
    @Column(name = "days_count", nullable = false) private int daysCount;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal quantity;
    @Column(name = "unit_code", nullable = false, length = 40) private String unitCode;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "created_by", nullable = false, length = 80) private String createdBy;
    @Column(name = "updated_by", nullable = false, length = 80) private String updatedBy;

    protected PrescriptionItem() {}

    public static PrescriptionItem create(UUID prescriptionId, int lineNo, UUID medicationId,
            String medicationLabel, String dose, String route, String frequency, int daysCount,
            BigDecimal quantity, String unitCode, String actor) {
        PrescriptionItem i = new PrescriptionItem();
        OffsetDateTime now = OffsetDateTime.now();
        i.id = new Key(prescriptionId, lineNo);
        i.medicationId = medicationId;
        i.medicationLabel = medicationLabel;
        i.dose = dose;
        i.route = route;
        i.frequency = frequency;
        i.daysCount = daysCount;
        i.quantity = quantity;
        i.unitCode = unitCode;
        i.createdAt = now;
        i.updatedAt = now;
        i.createdBy = actor;
        i.updatedBy = actor;
        return i;
    }

    public int getLineNo() { return id.getLineNo(); }
    public UUID getMedicationId() { return medicationId; }
    public String getMedicationLabel() { return medicationLabel; }
    public String getDose() { return dose; }
    public String getRoute() { return route; }
    public String getFrequency() { return frequency; }
    public int getDaysCount() { return daysCount; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnitCode() { return unitCode; }
}