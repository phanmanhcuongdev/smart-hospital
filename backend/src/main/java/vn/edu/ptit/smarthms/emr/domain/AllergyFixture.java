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

/** Read-only view of recorded allergies (ref.allergy_fixture, FR-EMR-03). */
@Entity
@Table(schema = "ref", name = "allergy_fixture")
public class AllergyFixture extends AuditedEntity {

    public enum Status { UNKNOWN, NONE_CONFIRMED, HAS_ALLERGY }

    @Id
    @Column(name = "allergy_id", nullable = false)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "allergen", length = 240)
    private String allergen;

    @Enumerated(EnumType.STRING)
    @Column(name = "allergy_status", nullable = false, length = 24)
    private Status status;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by", length = 80)
    private String verifiedBy;

    @Column(name = "source_version", nullable = false, length = 40)
    private String sourceVersion;

    protected AllergyFixture() {
    }

    public UUID getId() { return id; }
    public UUID getPatientId() { return patientId; }
    public String getAllergen() { return allergen; }
    public Status getStatus() { return status; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public String getVerifiedBy() { return verifiedBy; }
}
