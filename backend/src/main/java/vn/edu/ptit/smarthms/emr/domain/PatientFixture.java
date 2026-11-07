package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** Read-only view of a patient identity (ref.patient_fixture, FR-EMR-01). */
@Entity
@Table(schema = "ref", name = "patient_fixture")
public class PatientFixture extends AuditedEntity {

    public enum IdentityStatus { UNVERIFIED, VERIFIED, FLAGGED }

    @Id
    @Column(name = "patient_id", nullable = false)
    private UUID id;

    @Column(name = "display_name", length = 240)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_status", nullable = false, length = 16)
    private IdentityStatus identityStatus;

    protected PatientFixture() {
    }

    public UUID getId() { return id; }
    public String getDisplayName() { return displayName; }
    public IdentityStatus getIdentityStatus() { return identityStatus; }
}
