package vn.edu.ptit.smarthms.system.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.system.domain.AuditEvent;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByActorRefOrderByHappenedAtDesc(String actorRef);
    List<AuditEvent> findByTargetTypeAndTargetIdOrderByHappenedAtDesc(String targetType, String targetId);
    List<AuditEvent> findByOutcome(String outcome);
    List<AuditEvent> findByAction(String action);
}
