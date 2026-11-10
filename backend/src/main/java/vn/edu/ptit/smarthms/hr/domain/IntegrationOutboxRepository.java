package vn.edu.ptit.smarthms.hr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationOutboxRepository extends JpaRepository<IntegrationOutbox, UUID> {
    boolean existsByEventKey(String eventKey);
    List<IntegrationOutbox> findByDeliveryState(IntegrationOutbox.DeliveryState deliveryState);
}
