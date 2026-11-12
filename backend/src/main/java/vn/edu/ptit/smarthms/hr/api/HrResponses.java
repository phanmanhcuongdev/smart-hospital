package vn.edu.ptit.smarthms.hr.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import vn.edu.ptit.smarthms.hr.domain.Department;
import vn.edu.ptit.smarthms.hr.domain.Employee;
import vn.edu.ptit.smarthms.hr.domain.EmployeeStatusHistory;
import vn.edu.ptit.smarthms.hr.domain.Employment;
import vn.edu.ptit.smarthms.hr.domain.LicenseCertification;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecision;
import vn.edu.ptit.smarthms.hr.domain.RoleProfile;

/** HR response DTOs. Patient data is never exposed here; employee data is internal. */
public final class HrResponses {

    private HrResponses() {
    }

    public record DepartmentResponse(UUID id, String departmentCode, String displayName,
            UUID parentDepartmentId, boolean active, String departmentType,
            Instant createdAt, String createdBy) {
        public static DepartmentResponse from(Department department) {
            return new DepartmentResponse(department.getId(), department.getDepartmentCode(),
                    department.getDisplayName(), department.getParentDepartmentId(),
                    department.isActive(), department.getDepartmentType(),
                    department.getCreatedAt(), department.getCreatedBy());
        }
    }

    public record EmployeeResponse(UUID id, String employeeCode, String fullName,
            LocalDate birthDate, String status, Instant statusEffectiveAt,
            String gender, String address, String phone, String email, LocalDate hireDate,
            Instant createdAt, String createdBy) {
        public static EmployeeResponse from(Employee employee) {
            return new EmployeeResponse(employee.getId(), employee.getEmployeeCode(),
                    employee.getFullName(), employee.getBirthDate(),
                    employee.getStatus().name(), employee.getStatusEffectiveAt(),
                    employee.getGender(), employee.getAddress(), employee.getPhone(),
                    employee.getEmail(), employee.getHireDate(),
                    employee.getCreatedAt(), employee.getCreatedBy());
        }
    }

    public record AssignmentResponse(UUID id, UUID employeeId, UUID departmentId,
            String departmentCode, String jobTitle, UUID roleProfileId,
            Instant startAt, Instant endAt, String status) {
        public static AssignmentResponse from(Employment employment, String departmentCode) {
            return new AssignmentResponse(employment.getId(), employment.getEmployeeId(),
                    employment.getDepartmentId(), departmentCode,
                    employment.getJobTitle(), employment.getRoleProfileId(),
                    employment.getStartAt(), employment.getEndAt(),
                    employment.isOpen() ? "OPEN" : "CLOSED");
        }
    }

    public record CertificationResponse(UUID id, UUID employeeId, String credentialType,
            String credentialNo, String issuer, String scopeNote,
            LocalDate validFrom, LocalDate validUntil, String verificationStatus,
            String validity, String sourceRef, Instant createdAt, String createdBy) {
        public static CertificationResponse from(LicenseCertification certification,
                String validity) {
            return new CertificationResponse(certification.getId(), certification.getEmployeeId(),
                    certification.getCredentialType(), certification.getCredentialNo(),
                    certification.getIssuer(), certification.getScopeNote(),
                    certification.getValidFrom(), certification.getValidUntil(),
                    certification.getVerificationStatus().name(), validity,
                    certification.getSourceRef(), certification.getCreatedAt(),
                    certification.getCreatedBy());
        }
    }

    public record DecisionResponse(UUID id, UUID employeeId, String decisionRef,
            String decisionType, String decisionStatus, String executionStatus,
            Instant effectiveAt, String approvedBy, Instant approvedAt, String reason,
            Instant createdAt, String createdBy) {
        public static DecisionResponse from(PersonnelDecision decision) {
            return new DecisionResponse(decision.getId(), decision.getEmployeeId(),
                    decision.getDecisionRef(), decision.getDecisionType().name(),
                    decision.getDecisionStatus().name(), decision.getExecutionStatus().name(),
                    decision.getEffectiveAt(), decision.getApprovedBy(),
                    decision.getApprovedAt(), decision.getReason(),
                    decision.getCreatedAt(), decision.getCreatedBy());
        }
    }

    public record RoleProfileResponse(UUID id, String titleName, String professionalGroup,
            String qualification, String description, Instant createdAt, String createdBy) {
        public static RoleProfileResponse from(RoleProfile roleProfile) {
            return new RoleProfileResponse(roleProfile.getId(), roleProfile.getTitleName(),
                    roleProfile.getProfessionalGroup(), roleProfile.getQualification(),
                    roleProfile.getDescription(), roleProfile.getCreatedAt(),
                    roleProfile.getCreatedBy());
        }
    }

    public record EmployeeStatusHistoryResponse(UUID id, UUID employeeId, String oldStatus,
            String newStatus, Instant effectiveAt, UUID decisionId, Instant createdAt) {
        public static EmployeeStatusHistoryResponse from(EmployeeStatusHistory history) {
            return new EmployeeStatusHistoryResponse(history.getId(), history.getEmployeeId(),
                    history.getOldStatus() == null ? null : history.getOldStatus().name(),
                    history.getNewStatus().name(), history.getEffectiveAt(),
                    history.getDecisionId(), history.getCreatedAt());
        }
    }
}
