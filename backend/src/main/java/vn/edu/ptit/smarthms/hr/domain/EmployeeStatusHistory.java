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
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** Employee status history (hr.employee_status_history, FR-HR-03). History is never overwritten. */
@Entity
@Table(name = "employee_status_history", schema = "hr")
public class EmployeeStatusHistory extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "history_id")
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private Employee.Status oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private Employee.Status newStatus;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(name = "decision_id")
    private UUID decisionId;

    protected EmployeeStatusHistory() {
    }

    public EmployeeStatusHistory(UUID employeeId, Employee.Status oldStatus,
            Employee.Status newStatus, Instant effectiveAt, UUID decisionId) {
        this.employeeId = employeeId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.effectiveAt = effectiveAt;
        this.decisionId = decisionId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public Employee.Status getOldStatus() {
        return oldStatus;
    }

    public Employee.Status getNewStatus() {
        return newStatus;
    }

    public Instant getEffectiveAt() {
        return effectiveAt;
    }

    public UUID getDecisionId() {
        return decisionId;
    }
}
