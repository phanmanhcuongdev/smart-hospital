package vn.edu.ptit.smarthms.system.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.system.domain.AuditEvent;
import vn.edu.ptit.smarthms.system.repository.AuditEventRepository;

@Service
public class AuditService {

    private static final Set<String> REDACT_KEYS = Set.of(
            "password", "token", "secret", "authorization", "bearer", "creditcard", "ssn", "phi_detail"
    );

    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "\"(password|token|secret|authorization|bearer|creditcard|ssn)\"\\s*:\\s*\"[^\"]+\"",
            Pattern.CASE_INSENSITIVE
    );

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuditEvent record(UUID actorUserId, String actorRef, String targetType, String targetId,
                             String action, AuditEvent.Outcome outcome, String changeRef,
                             String rawMetadata) {
        String sanitized = sanitize(rawMetadata);
        AuditEvent event = AuditEvent.create(
                actorUserId, actorRef, targetType, targetId, action, outcome, changeRef, sanitized, actorRef);
        return repository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> queryByActor(String actorRef) {
        return repository.findByActorRefOrderByHappenedAtDesc(actorRef);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> queryByTarget(String targetType, String targetId) {
        return repository.findByTargetTypeAndTargetIdOrderByHappenedAtDesc(targetType, targetId);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> queryByOutcome(AuditEvent.Outcome outcome) {
        return repository.findByOutcome(outcome.name());
    }

    public String sanitize(String json) {
        if (json == null || json.isBlank()) {
            return "{}";
        }
        return SENSITIVE_PATTERN.matcher(json).replaceAll("\"$1\":\"[REDACTED]\"");
    }
}
