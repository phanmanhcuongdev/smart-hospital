package vn.edu.ptit.smarthms.hr.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import vn.edu.ptit.smarthms.common.domain.AuditedEntity;

/** Employee (hr.employee, FR-HR-01, FR-HR-03, BR-015 to BR-020). */
@Entity
@Table(name = "employee", schema = "hr")
public class Employee extends AuditedEntity {

    public enum Status {
        ONBOARDING, ACTIVE, SUSPENDED, TERMINATED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "employee_id")
    private UUID id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 40)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ONBOARDING;

    @Column(name = "status_effective_at", nullable = false)
    private Instant statusEffectiveAt = Instant.now();

    @Version
    @Column(name = "row_version", nullable = false)
    private Integer rowVersion = 1;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "address")
    private String address;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    protected Employee() {
    }

    public Employee(String employeeCode, String fullName) {
        this.employeeCode = employeeCode;
        this.fullName = fullName;
    }

    public UUID getId() {
        return id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getStatusEffectiveAt() {
        return statusEffectiveAt;
    }

    public void setStatusEffectiveAt(Instant statusEffectiveAt) {
        this.statusEffectiveAt = statusEffectiveAt;
    }

    public Integer getRowVersion() {
        return rowVersion;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }
}
