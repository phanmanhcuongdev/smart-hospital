package vn.edu.ptit.smarthms.system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "sys", name = "audit_event")
public class AuditEvent {

    public enum Outcome {
        SUCCESS, DENIED, FAILURE
    }

    @Id
    @Column(name = "audit_id")
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
    private OffsetDateTime happenedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "safe_metadata")
    private String safeMetadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false, length = 80)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 80)
    private String updatedBy;

    protected AuditEvent() {}

    public static AuditEvent create(UUID actorUserId, String actorRef, String targetType,
                                    String targetId, String action, Outcome outcome,
                                    String changeRef, String safeMetadata, String creator) {
        AuditEvent a = new AuditEvent();
        OffsetDateTime now = OffsetDateTime.now();
        a.auditId = UUID.randomUUID();
        a.actorUserId = actorUserId;
        a.actorRef = actorRef != null ? actorRef : "unknown";
        a.targetType = targetType;
        a.targetId = targetId;
        a.action = action;
        a.outcome = outcome.name();
        a.changeRef = changeRef;
        a.happenedAt = now;
        a.safeMetadata = safeMetadata;
        a.createdAt = now;
        a.updatedAt = now;
        a.createdBy = creator != null ? creator : a.actorRef;
        a.updatedBy = creator != null ? creator : a.actorRef;
        return a;
    }

    public UUID getAuditId() { return auditId; }
    public UUID getActorUserId() { return actorUserId; }
    public String getActorRef() { return actorRef; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getAction() { return action; }
    public String getOutcome() { return outcome; }
    public String getChangeRef() { return changeRef; }
    public OffsetDateTime getHappenedAt() { return happenedAt; }
    public String getSafeMetadata() { return safeMetadata; }
}
