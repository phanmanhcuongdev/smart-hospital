package vn.edu.ptit.smarthms.system.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_account", schema = "sys")
public class UserAccount extends AuditableEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;

    @Column(name = "account_status", nullable = false, length = 16)
    private String accountStatus;

    @Column(name = "revocation_epoch", nullable = false)
    private int revocationEpoch;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<RoleAssignment> roleAssignments = new ArrayList<>();

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    public int getRevocationEpoch() { return revocationEpoch; }
    public void setRevocationEpoch(int revocationEpoch) { this.revocationEpoch = revocationEpoch; }
    public List<RoleAssignment> getRoleAssignments() { return roleAssignments; }
    public void setRoleAssignments(List<RoleAssignment> roleAssignments) { this.roleAssignments = roleAssignments; }
}
