package vn.edu.ptit.smarthms.hr.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** Integration outbox event (hr.integration_outbox, FR-HR-03, BR-020, BR-090). */
@Entity
@Table(name = "integration_outbox", schema = "hr")
public class IntegrationOutbox extends AuditedEntity {

    public enum DeliveryState {
        PENDING, SENT, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id")
    private UUID id;

    @Column(name = "event_key", nullable = false, unique = true, length = 200)
    private String eventKey;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_state", nullable = false, length = 16)
    private DeliveryState deliveryState = DeliveryState.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    protected IntegrationOutbox() {
    }

    public IntegrationOutbox(String eventKey, UUID employeeId, String payload) {
        this.eventKey = eventKey;
        this.employeeId = employeeId;
        this.payload = payload;
    }

    public UUID getId() {
        return id;
    }

    public String getEventKey() {
        return eventKey;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public String getPayload() {
        return payload;
    }

    public DeliveryState getDeliveryState() {
        return deliveryState;
    }

    public void setDeliveryState(DeliveryState deliveryState) {
        this.deliveryState = deliveryState;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(Instant lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }
}
