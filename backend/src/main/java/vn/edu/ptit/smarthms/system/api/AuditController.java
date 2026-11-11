package vn.edu.ptit.smarthms.system.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.common.annotation.RequiresPermission;
import vn.edu.ptit.smarthms.common.audit.AuditService;
import vn.edu.ptit.smarthms.common.error.AppException;
import vn.edu.ptit.smarthms.common.error.ErrorCode;
import vn.edu.ptit.smarthms.common.util.SecurityUtils;
import vn.edu.ptit.smarthms.system.domain.AuditEvent;
import vn.edu.ptit.smarthms.system.repository.AuditEventRepository;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditEventRepository auditEventRepository;
    private final AuditService auditService;

    public AuditController(AuditEventRepository auditEventRepository, AuditService auditService) {
        this.auditEventRepository = auditEventRepository;
        this.auditService = auditService;
    }

    @GetMapping("/events")
    @RequiresPermission(resource = "AUDIT_EVENT", action = "READ", scope = "ALL")
    public ResponseEntity<List<AuditEventResponse>> listRecentEvents() {
        List<AuditEventResponse> events = auditEventRepository
                .findAll(PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "happenedAt")))
                .stream()
                .map(AuditEventResponse::from)
                .toList();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/{auditId}")
    @RequiresPermission(resource = "AUDIT_EVENT", action = "READ", scope = "ALL")
    public ResponseEntity<AuditEventResponse> getEvent(@PathVariable UUID auditId) {
        AuditEvent event = auditEventRepository.findById(auditId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Audit event not found: " + auditId));
        return ResponseEntity.ok(AuditEventResponse.from(event));
    }

    @PutMapping("/events/{auditId}")
    public ResponseEntity<Void> rejectUpdate(@PathVariable UUID auditId,
                                             @RequestBody(required = false) Map<String, Object> body) {
        auditService.denied(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(),
                "AUDIT_EVENT",
                auditId.toString(),
                "UPDATE_AUDIT",
                Map.of("reason", "APPEND_ONLY_POLICY_BR002_NFR_LOG_01")
        );
        throw new AppException(ErrorCode.AUDIT_IMMUTABLE);
    }

    @DeleteMapping("/events/{auditId}")
    public ResponseEntity<Void> rejectDelete(@PathVariable UUID auditId) {
        auditService.denied(
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.getCurrentUsername(),
                "AUDIT_EVENT",
                auditId.toString(),
                "DELETE_AUDIT",
                Map.of("reason", "APPEND_ONLY_POLICY_BR002_NFR_LOG_01")
        );
        throw new AppException(ErrorCode.AUDIT_IMMUTABLE);
    }
}
