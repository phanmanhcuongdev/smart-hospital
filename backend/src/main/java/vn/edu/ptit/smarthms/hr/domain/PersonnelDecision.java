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

/** Personnel decision (hr.personnel_decision, FR-HR-03, FR-HR-04, BR-017, BR-019, BR-020). */
@Entity
@Table(name = "personnel_decision", schema = "hr")
public class PersonnelDecision extends AuditedEntity {

    public enum DecisionType {
        ACTIVATE, SUSPEND, RETURN, TERMINATE, TRANSFER
    }

    public enum DecisionStatus {
        DRAFT, APPROVED, REVOKED
    }

    public enum ExecutionStatus {
        PENDING, APPLIED, SKIPPED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "decision_id")
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "decision_ref", nullable = false, unique = true, length = 120)
    private String decisionRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 20)
    private DecisionType decisionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_status", nullable = false, length = 16)
    private DecisionStatus decisionStatus = DecisionStatus.DRAFT;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(name = "approved_by", length = 80)
    private String approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 16)
    private ExecutionStatus executionStatus = ExecutionStatus.PENDING;

    protected PersonnelDecision() {
    }

    public PersonnelDecision(UUID employeeId, String decisionRef,
            DecisionType decisionType, Instant effectiveAt) {
        this.employeeId = employeeId;
        this.decisionRef = decisionRef;
        this.decisionType = decisionType;
        this.effectiveAt = effectiveAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public String getDecisionRef() {
        return decisionRef;
    }

    public void setDecisionRef(String decisionRef) {
        this.decisionRef = decisionRef;
    }

    public DecisionType getDecisionType() {
        return decisionType;
    }

    public void setDecisionType(DecisionType decisionType) {
        this.decisionType = decisionType;
    }

    public DecisionStatus getDecisionStatus() {
        return decisionStatus;
    }

    public void setDecisionStatus(DecisionStatus decisionStatus) {
        this.decisionStatus = decisionStatus;
    }

    public Instant getEffectiveAt() {
        return effectiveAt;
    }

    public void setEffectiveAt(Instant effectiveAt) {
        this.effectiveAt = effectiveAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(ExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }
}
