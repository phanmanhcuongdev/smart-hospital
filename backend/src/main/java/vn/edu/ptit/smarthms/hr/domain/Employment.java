package vn.edu.ptit.smarthms.hr.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** Employment assignment (hr.employment, FR-HR-01, FR-HR-04). Open while endAt is null. */
@Entity
@Table(name = "employment", schema = "hr")
public class Employment extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "employment_id")
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "department_id", nullable = false)
    private UUID departmentId;

    @Column(name = "job_title", nullable = false, length = 120)
    private String jobTitle;

    @Column(name = "role_profile_id")
    private UUID roleProfileId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "decision_ref", length = 120)
    private String decisionRef;

    protected Employment() {
    }

    public Employment(UUID employeeId, UUID departmentId, String jobTitle, Instant startAt) {
        this.employeeId = employeeId;
        this.departmentId = departmentId;
        this.jobTitle = jobTitle;
        this.startAt = startAt;
    }

    public boolean isOpen() {
        return endAt == null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public UUID getRoleProfileId() {
        return roleProfileId;
    }

    public void setRoleProfileId(UUID roleProfileId) {
        this.roleProfileId = roleProfileId;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public String getDecisionRef() {
        return decisionRef;
    }

    public void setDecisionRef(String decisionRef) {
        this.decisionRef = decisionRef;
    }
}
