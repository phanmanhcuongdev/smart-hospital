package vn.edu.ptit.smarthms.emr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** Read-only view of the medication catalog (ref.medication_fixture, FR-EMR-03). */
@Entity
@Table(schema = "ref", name = "medication_fixture")
public class MedicationFixture extends AuditedEntity {

    @Id
    @Column(name = "medication_id", nullable = false)
    private UUID id;

    @Column(name = "display_name", nullable = false, length = 240)
    private String displayName;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "source_version", nullable = false, length = 40)
    private String sourceVersion;

    protected MedicationFixture() {
    }

    public UUID getId() { return id; }
    public String getDisplayName() { return displayName; }
    public boolean isActive() { return active; }
    public String getSourceVersion() { return sourceVersion; }
}
