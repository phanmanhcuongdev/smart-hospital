package vn.edu.ptit.smarthms.system.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.system.domain.AuditEvent;
import vn.edu.ptit.smarthms.system.repository.AuditEventRepository;

/** Read-only view of sys.audit_event (merged from the phung-nam branch). */
@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    public record AuditEventResponse(UUID auditId, UUID actorUserId, String actorRef, String targetType,
                                     String targetId, String action, String outcome,
                                     OffsetDateTime happenedAt, String safeMetadata) {
        static AuditEventResponse from(AuditEvent e) {
            return new AuditEventResponse(e.getAuditId(), e.getActorUserId(), e.getActorRef(), e.getTargetType(),
                    e.getTargetId(), e.getAction(), e.getOutcome(), e.getHappenedAt(), e.getSafeMetadata());
        }
    }

    private final AuditEventRepository repository;

    public AuditController(AuditEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/events")
    @PreAuthorize("hasAuthority('AUDIT_EVENT:READ:ALL')")
    public List<AuditEventResponse> events(@RequestParam(defaultValue = "100") int limit) {
        int size = Math.max(1, Math.min(limit, 500));
        return repository.findAll(PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "happenedAt")))
                .map(AuditEventResponse::from)
                .getContent();
    }
}
