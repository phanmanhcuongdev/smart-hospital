package vn.edu.ptit.smarthms.hr.domain;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** Department (hr.department, FR-HR-01, BR-015). */
@Entity
@Table(name = "department", schema = "hr")
public class Department extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "department_id")
    private UUID id;

    @Column(name = "department_code", nullable = false, unique = true, length = 40)
    private String departmentCode;

    @Column(name = "display_name", nullable = false, length = 180)
    private String displayName;

    @Column(name = "parent_department_id")
    private UUID parentDepartmentId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "department_type", length = 60)
    private String departmentType;

    protected Department() {
    }

    public Department(String departmentCode, String displayName) {
        this.departmentCode = departmentCode;
        this.displayName = displayName;
    }

    public UUID getId() {
        return id;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UUID getParentDepartmentId() {
        return parentDepartmentId;
    }

    public void setParentDepartmentId(UUID parentDepartmentId) {
        this.parentDepartmentId = parentDepartmentId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(String departmentType) {
        this.departmentType = departmentType;
    }
}
