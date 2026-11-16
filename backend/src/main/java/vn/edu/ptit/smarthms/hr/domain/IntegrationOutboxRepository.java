/**
 * Mô tả: Repository JPA truy vấn các sự kiện tích hợp (outbox) của module HR
 * Người thực hiện: Tran Hoang Nam
 * Ngày thực hiện: 20/10/2026
 */

package vn.edu.ptit.smarthms.hr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationOutboxRepository extends JpaRepository<IntegrationOutbox, UUID> {
    boolean existsByEventKey(String eventKey);
    List<IntegrationOutbox> findByDeliveryState(IntegrationOutbox.DeliveryState deliveryState);
}
