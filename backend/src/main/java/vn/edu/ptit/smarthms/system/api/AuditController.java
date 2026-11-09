package vn.edu.ptit.smarthms.system.api;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.common.annotation.RequiresPermission;
import vn.edu.ptit.smarthms.system.repository.AuditEventRepository;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditEventRepository auditEventRepository;

    public AuditController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping("/events")
    @RequiresPermission(resource = "AUDIT_EVENT", action = "READ", scope = "ALL")
    public ResponseEntity<List<AuditEventResponse>> listRecentEvents() {
        List<AuditEventResponse> events = auditEventRepository
                .findAll(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "happenedAt")))
                .stream()
                .map(AuditEventResponse::from)
                .toList();
        return ResponseEntity.ok(events);
    }
}
