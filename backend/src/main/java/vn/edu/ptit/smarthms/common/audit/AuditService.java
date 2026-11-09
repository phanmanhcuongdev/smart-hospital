package vn.edu.ptit.smarthms.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.system.domain.AuditEvent;
import vn.edu.ptit.smarthms.system.repository.AuditEventRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(UUID actorUserId, String actorRef, String targetType, String targetId,
                            String action, String outcome, String changeRef,
                            Map<String, Object> safeMetadata) {
        AuditEvent event = new AuditEvent();
        event.setAuditId(UUID.randomUUID());
        event.setActorUserId(actorUserId);
        event.setActorRef((actorRef != null && !actorRef.isBlank()) ? actorRef : "anonymous");
        event.setTargetType((targetType != null && !targetType.isBlank()) ? targetType : "UNKNOWN");
        event.setTargetId((targetId != null && !targetId.isBlank()) ? targetId : "N/A");
        event.setAction((action != null && !action.isBlank()) ? action : "UNKNOWN");
        event.setOutcome((outcome != null && !outcome.isBlank()) ? outcome : "FAILURE");
        event.setChangeRef(changeRef);
        event.setHappenedAt(Instant.now());
        if (safeMetadata != null && !safeMetadata.isEmpty()) {
            event.setSafeMetadata(toJson(safeMetadata));
        }
        auditEventRepository.save(event);
    }

    @Async
    public void recordEventAsync(UUID actorUserId, String actorRef, String targetType, String targetId,
                                 String action, String outcome, String changeRef,
                                 Map<String, Object> safeMetadata) {
        try {
            recordEvent(actorUserId, actorRef, targetType, targetId, action, outcome, changeRef, safeMetadata);
        } catch (Exception e) {
            log.error("Async audit write failed: actor={} action={} target={}", actorRef, action, targetId, e);
        }
    }

    public void success(UUID actorUserId, String actorRef, String targetType, String targetId,
                        String action, String changeRef, Map<String, Object> meta) {
        recordEventAsync(actorUserId, actorRef, targetType, targetId, action, "SUCCESS", changeRef, meta);
    }

    public void denied(UUID actorUserId, String actorRef, String targetType, String targetId,
                       String action, Map<String, Object> meta) {
        recordEventAsync(actorUserId, actorRef, targetType, targetId, action, "DENIED", null, meta);
    }

    public void failure(UUID actorUserId, String actorRef, String targetType, String targetId,
                        String action, Map<String, Object> meta) {
        recordEventAsync(actorUserId, actorRef, targetType, targetId, action, "FAILURE", null, meta);
    }

    /**
     * Simple Map-to-JSON serializer to avoid adding a Jackson dependency directly.
     * Uses String.format for basic key-value pairs.
     */
    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        String entries = map.entrySet().stream()
                .map(e -> "\"" + escape(String.valueOf(e.getKey())) + "\":"
                        + jsonValue(e.getValue()))
                .collect(Collectors.joining(","));
        return "{" + entries + "}";
    }

    private String jsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
