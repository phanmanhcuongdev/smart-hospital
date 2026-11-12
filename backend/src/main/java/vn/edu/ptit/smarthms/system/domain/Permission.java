package vn.edu.ptit.smarthms.system.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "permission", schema = "sys",
        uniqueConstraints = @UniqueConstraint(columnNames = {"resource_code", "action_code", "scope_code"}))
public class Permission extends AuditableEntity {

    @Id
    @Column(name = "permission_id", nullable = false, updatable = false)
    private UUID permissionId;

    @Column(name = "resource_code", nullable = false, length = 60)
    private String resourceCode;

    @Column(name = "action_code", nullable = false, length = 60)
    private String actionCode;

    @Column(name = "scope_code", nullable = false, length = 60)
    private String scopeCode;

    public UUID getPermissionId() { return permissionId; }
    public void setPermissionId(UUID permissionId) { this.permissionId = permissionId; }
    public String getResourceCode() { return resourceCode; }
    public void setResourceCode(String resourceCode) { this.resourceCode = resourceCode; }
    public String getActionCode() { return actionCode; }
    public void setActionCode(String actionCode) { this.actionCode = actionCode; }
    public String getScopeCode() { return scopeCode; }
    public void setScopeCode(String scopeCode) { this.scopeCode = scopeCode; }

    public String toAuthorityString() {
        return resourceCode + ":" + actionCode + ":" + scopeCode;
    }
}
