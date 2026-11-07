package vn.edu.ptit.smarthms.hr.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vn.edu.ptit.smarthms.hr.domain.LicenseCertification;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecision;

/** HR request DTOs. Creating an employee never grants a system role (BR-015). */
public final class HrRequests {

    private HrRequests() {
    }

    public record CreateEmployeeRequest(
            @NotBlank String employeeCode,
            @NotBlank String fullName,
            LocalDate birthDate,
            @NotNull UUID departmentId,
            @NotBlank String jobTitle,
            UUID roleProfileId,
            String gender,
            String address,
            String phone,
            @Email String email,
            LocalDate hireDate) {
    }

    public record UpdateEmployeeRequest(
            String fullName,
            LocalDate birthDate,
            String gender,
            String address,
            String phone,
            @Email String email) {
    }

    public record CreateDepartmentRequest(
            @NotBlank String departmentCode,
            @NotBlank String displayName,
            UUID parentDepartmentId,
            String departmentType,
            Boolean active) {
    }

    public record UpdateDepartmentRequest(
            String displayName,
            UUID parentDepartmentId,
            String departmentType,
            Boolean active) {
    }

    public record CreateRoleProfileRequest(
            @NotBlank String titleName,
            String professionalGroup,
            String qualification,
            String description) {
    }

    public record CreateCertificationRequest(
            @NotBlank String credentialType,
            String credentialNo,
            String issuer,
            String scopeNote,
            LocalDate validFrom,
            LocalDate validUntil,
            LicenseCertification.VerificationStatus verificationStatus,
            @NotBlank String sourceRef) {
    }

    public record UpdateCertificationRequest(
            String issuer,
            String scopeNote,
            LocalDate validFrom,
            LocalDate validUntil,
            LicenseCertification.VerificationStatus verificationStatus,
            String sourceRef) {
    }

    public record CreateDecisionRequest(
            @NotBlank String decisionRef,
            @NotNull PersonnelDecision.DecisionType decisionType,
            @NotNull Instant effectiveAt,
            String reason) {
    }

    public record CreateTransferRequest(
            @NotNull UUID departmentId,
            @NotBlank String jobTitle,
            UUID roleProfileId,
            @NotNull Instant effectiveFrom,
            String decisionRef) {
    }
}
