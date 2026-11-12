package vn.edu.ptit.smarthms.hr.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.hr.domain.IntegrationOutbox;
import vn.edu.ptit.smarthms.hr.domain.IntegrationOutboxRepository;

@Service
public class SchIntegrationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(SchIntegrationDispatcher.class);

    public interface SchedulerClient {
        void deliverEvent(String eventKey, String payload) throws TimeoutException, Exception;
    }

    private final IntegrationOutboxRepository outboxRepository;

    public SchIntegrationDispatcher(IntegrationOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public DispatchResult dispatchPendingEvents(SchedulerClient client) {
        List<IntegrationOutbox> pending = outboxRepository.findByDeliveryState(IntegrationOutbox.DeliveryState.PENDING);
        int successCount = 0;
        int failureCount = 0;
        int retryPendingCount = 0;

        for (IntegrationOutbox event : pending) {
            try {
                client.deliverEvent(event.getEventKey(), event.getPayload());
                event.setDeliveryState(IntegrationOutbox.DeliveryState.SENT);
                event.setLastAttemptAt(Instant.now());
                event.setAttempts(event.getAttempts() + 1);
                outboxRepository.save(event);
                successCount++;
            } catch (TimeoutException ex) {
                log.warn("Downstream SCH mock timed out for eventKey={}: {}", event.getEventKey(), ex.getMessage());
                event.setDeliveryState(IntegrationOutbox.DeliveryState.PENDING);
                event.setAttempts(event.getAttempts() + 1);
                event.setLastAttemptAt(Instant.now());
                outboxRepository.save(event);
                retryPendingCount++;
            } catch (Exception ex) {
                log.error("Failed delivering eventKey={}: {}", event.getEventKey(), ex.getMessage());
                event.setDeliveryState(IntegrationOutbox.DeliveryState.FAILED);
                event.setAttempts(event.getAttempts() + 1);
                event.setLastAttemptAt(Instant.now());
                outboxRepository.save(event);
                failureCount++;
            }
        }
        return new DispatchResult(pending.size(), successCount, retryPendingCount, failureCount);
    }

    public record DispatchResult(int totalProcessed, int successCount, int retryPendingCount, int failureCount) {}
}
