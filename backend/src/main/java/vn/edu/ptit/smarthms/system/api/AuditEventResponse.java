package vn.edu.ptit.smarthms.system.api;

import java.time.Instant;
import java.util.UUID;
import vn.edu.ptit.smarthms.system.domain.AuditEvent;

public record AuditEventResponse(
        UUID auditId,
        UUID actorUserId,
        String actorRef,
        String targetType,
        String targetId,
        String action,
        String outcome,
        Instant happenedAt,
        String safeMetadata
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getAuditId(),
                event.getActorUserId(),
                event.getActorRef(),
                event.getTargetType(),
                event.getTargetId(),
                event.getAction(),
                event.getOutcome(),
                event.getHappenedAt(),
                event.getSafeMetadata()
        );
    }
}
