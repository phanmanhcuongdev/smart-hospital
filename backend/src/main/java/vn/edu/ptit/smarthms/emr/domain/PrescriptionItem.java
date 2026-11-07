package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** One line of a prescription. Maps to emr.prescription_item (FR-EMR-03). */
@Entity
@Table(schema = "emr", name = "prescription_item")
public class PrescriptionItem extends AuditedEntity {

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "prescription_id", nullable = false)
        private UUID prescriptionId;
        @Column(name = "line_no", nullable = false)
        private Integer lineNo;
        protected Key() {
        }
        public Key(UUID prescriptionId, Integer lineNo) {
            this.prescriptionId = prescriptionId;
            this.lineNo = lineNo;
        }
        public UUID getPrescriptionId() { return prescriptionId; }
        public Integer getLineNo() { return lineNo; }
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            Key key = (Key) other;
            return Objects.equals(prescriptionId, key.prescriptionId) && Objects.equals(lineNo, key.lineNo);
        }
        @Override
        public int hashCode() {
            return Objects.hash(prescriptionId, lineNo);
        }
    }

    @EmbeddedId
    private Key id;

    @Column(name = "medication_id", nullable = false)
    private UUID medicationId;

    @Column(name = "medication_label", nullable = false, length = 240)
    private String medicationLabel;

    @Column(name = "dose", nullable = false, length = 100)
    private String dose;

    @Column(name = "route", nullable = false, length = 80)
    private String route;

    @Column(name = "frequency", nullable = false, length = 120)
    private String frequency;

    @Column(name = "days_count", nullable = false)
    private Integer daysCount;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_code", nullable = false, length = 40)
    private String unitCode;

    protected PrescriptionItem() {
    }

    public PrescriptionItem(UUID prescriptionId, Integer lineNo, UUID medicationId, String medicationLabel,
            String dose, String route, String frequency, Integer daysCount, BigDecimal quantity, String unitCode) {
        this.id = new Key(prescriptionId, lineNo);
        this.medicationId = medicationId;
        this.medicationLabel = medicationLabel;
        this.dose = dose;
        this.route = route;
        this.frequency = frequency;
        this.daysCount = daysCount;
        this.quantity = quantity;
        this.unitCode = unitCode;
    }

    public Key getId() { return id; }
    public UUID getMedicationId() { return medicationId; }
    public String getMedicationLabel() { return medicationLabel; }
    public String getDose() { return dose; }
    public String getRoute() { return route; }
    public String getFrequency() { return frequency; }
    public Integer getDaysCount() { return daysCount; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnitCode() { return unitCode; }
}
