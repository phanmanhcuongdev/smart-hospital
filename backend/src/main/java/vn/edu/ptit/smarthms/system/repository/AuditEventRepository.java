package vn.edu.ptit.smarthms.system.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.system.domain.AuditEvent;

/**
 * Mô tả: Repository tra cứu sự kiện kiểm toán theo đối tượng tác động, tác nhân hoặc trạng thái kết quả
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findByActorRefOrderByHappenedAtDesc(String actorRef);
    List<AuditEvent> findByTargetTypeAndTargetIdOrderByHappenedAtDesc(String targetType, String targetId);
    List<AuditEvent> findByOutcome(String outcome);
    List<AuditEvent> findByAction(String action);
}
