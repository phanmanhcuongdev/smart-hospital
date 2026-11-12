package vn.edu.ptit.smarthms.system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

/**
 * Audit columns shared by the SYS account tables (user_account, role, permission,
 * role_assignment, session). Ported from the phung-nam branch.
 */
@MappedSuperclass
public abstract class AuditableEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 80)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 80)
    private String updatedBy;

    @PrePersist
    protected void onPrePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.createdBy == null || this.createdBy.isBlank()) {
            this.createdBy = "system";
        }
        if (this.updatedBy == null || this.updatedBy.isBlank()) {
            this.updatedBy = this.createdBy;
        }
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.updatedAt = Instant.now();
        if (this.updatedBy == null || this.updatedBy.isBlank()) {
            this.updatedBy = "system";
        }
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
