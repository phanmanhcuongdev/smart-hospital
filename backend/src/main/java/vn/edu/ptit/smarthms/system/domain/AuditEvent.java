package vn.edu.ptit.smarthms.system.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event", schema = "sys")
public class AuditEvent extends AuditableEntity {

    @Id
    @Column(name = "audit_id", nullable = false, updatable = false)
    private UUID auditId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_ref", nullable = false, length = 80)
    private String actorRef;

    @Column(name = "target_type", nullable = false, length = 80)
    private String targetType;

    @Column(name = "target_id", nullable = false, length = 120)
    private String targetId;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "outcome", nullable = false, length = 16)
    private String outcome;

    @Column(name = "change_ref", length = 120)
    private String changeRef;

    @Column(name = "happened_at", nullable = false)
    private Instant happenedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "safe_metadata", columnDefinition = "jsonb")
    private String safeMetadata;

    public UUID getAuditId() { return auditId; }
    public void setAuditId(UUID auditId) { this.auditId = auditId; }
    public UUID getActorUserId() { return actorUserId; }
    public void setActorUserId(UUID actorUserId) { this.actorUserId = actorUserId; }
    public String getActorRef() { return actorRef; }
    public void setActorRef(String actorRef) { this.actorRef = actorRef; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getChangeRef() { return changeRef; }
    public void setChangeRef(String changeRef) { this.changeRef = changeRef; }
    public Instant getHappenedAt() { return happenedAt; }
    public void setHappenedAt(Instant happenedAt) { this.happenedAt = happenedAt; }
    public String getSafeMetadata() { return safeMetadata; }
    public void setSafeMetadata(String safeMetadata) { this.safeMetadata = safeMetadata; }
}
