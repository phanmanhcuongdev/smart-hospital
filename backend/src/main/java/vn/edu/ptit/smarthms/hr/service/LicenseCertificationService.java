package vn.edu.ptit.smarthms.hr.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.common.error.NotFoundException;
import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.hr.api.HrRequests.CreateCertificationRequest;
import vn.edu.ptit.smarthms.hr.api.HrRequests.UpdateCertificationRequest;
import vn.edu.ptit.smarthms.hr.api.HrResponses.CertificationResponse;
import vn.edu.ptit.smarthms.hr.domain.EmployeeRepository;
import vn.edu.ptit.smarthms.hr.domain.LicenseCertification;
import vn.edu.ptit.smarthms.hr.domain.LicenseCertificationRepository;

/** License and CCHN verification (FR-HR-02, BR-017, BR-018). */
@Service
@Transactional
public class LicenseCertificationService {

    public static final String CCHN_TYPE = "CCHN";

    private final LicenseCertificationRepository certifications;
    private final EmployeeRepository employees;

    public LicenseCertificationService(LicenseCertificationRepository certifications,
            EmployeeRepository employees) {
        this.certifications = certifications;
        this.employees = employees;
    }

    public CertificationResponse addCertification(Actor actor, UUID employeeId,
            CreateCertificationRequest request) {
        actor.requireRole("HR_SPECIALIST");
        requireEmployee(employeeId);
        if (request.validFrom() != null && request.validUntil() != null
                && request.validUntil().isBefore(request.validFrom())) {
            throw new ValidationException("expiryDate must not precede issueDate.");
        }
        LicenseCertification certification = new LicenseCertification(employeeId,
                request.credentialType(), request.sourceRef());
        certification.setCredentialNo(request.credentialNo());
        certification.setIssuer(request.issuer());
        certification.setScopeNote(request.scopeNote());
        certification.setValidFrom(request.validFrom());
        certification.setValidUntil(request.validUntil());
        if (request.verificationStatus() != null) {
            certification.setVerificationStatus(request.verificationStatus());
        }
        certification.setCreatedBy(actor.principalName());
        certification.setUpdatedBy(actor.principalName());
        LicenseCertification saved = certifications.save(certification);
        return CertificationResponse.from(saved, computeValidity(saved, LocalDate.now()));
    }

    public CertificationResponse updateCertification(Actor actor, UUID certificationId,
            UpdateCertificationRequest request) {
        actor.requireRole("HR_SPECIALIST");
        LicenseCertification certification = requireCertification(certificationId);
        if (request.issuer() != null) {
            certification.setIssuer(request.issuer());
        }
        if (request.scopeNote() != null) {
            certification.setScopeNote(request.scopeNote());
        }
        if (request.validFrom() != null) {
            certification.setValidFrom(request.validFrom());
        }
        if (request.validUntil() != null) {
            certification.setValidUntil(request.validUntil());
        }
        if (certification.getValidFrom() != null && certification.getValidUntil() != null
                && certification.getValidUntil().isBefore(certification.getValidFrom())) {
            throw new ValidationException("expiryDate must not precede issueDate.");
        }
        if (request.verificationStatus() != null) {
            certification.setVerificationStatus(request.verificationStatus());
        }
        if (request.sourceRef() != null) {
            certification.setSourceRef(request.sourceRef());
        }
        certification.setUpdatedBy(actor.principalName());
        LicenseCertification saved = certifications.save(certification);
        return CertificationResponse.from(saved, computeValidity(saved, LocalDate.now()));
    }

    public CertificationResponse getCertification(Actor actor, UUID certificationId) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        LicenseCertification certification = requireCertification(certificationId);
        return CertificationResponse.from(certification, computeValidity(certification, LocalDate.now()));
    }

    public List<CertificationResponse> listCertifications(Actor actor, UUID employeeId) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        requireEmployee(employeeId);
        LocalDate today = LocalDate.now();
        List<CertificationResponse> result = new ArrayList<>();
        for (LicenseCertification certification : certifications.findByEmployeeId(employeeId)) {
            result.add(CertificationResponse.from(certification, computeValidity(certification, today)));
        }
        return result;
    }

    public boolean hasValidCchn(UUID employeeId, LocalDate today) {
        String validity = cchnValidityFor(employeeId, today);
        return "VALID".equals(validity) || "EXPIRING_SOON".equals(validity);
    }

    public String cchnValidityFor(UUID employeeId, LocalDate today) {
        LicenseCertification latest = latestCchn(employeeId);
        if (latest == null) {
            return "UNVERIFIED";
        }
        return computeValidity(latest, today);
    }

    public static String computeValidity(LicenseCertification certification, LocalDate today) {
        if (certification.getVerificationStatus()
                == LicenseCertification.VerificationStatus.REJECTED
                || certification.getVerificationStatus() == LicenseCertification.VerificationStatus.REVOKED) {
            return "REJECTED";
        }
        if (certification.getVerificationStatus() == LicenseCertification.VerificationStatus.UNVERIFIED) {
            return "UNVERIFIED";
        }
        if (certification.getValidUntil() == null) {
            return "VALID";
        }
        if (certification.getValidUntil().isBefore(today)) {
            return "EXPIRED";
        }
        long daysLeft = ChronoUnit.DAYS.between(today, certification.getValidUntil());
        if (daysLeft <= 30) {
            return "EXPIRING_SOON";
        }
        return "VALID";
    }

    private LicenseCertification latestCchn(UUID employeeId) {
        LicenseCertification latest = null;
        for (LicenseCertification certification
                : certifications.findByEmployeeIdAndCredentialType(employeeId, CCHN_TYPE)) {
            if (latest == null || certification.getCreatedAt() == null
                    || (latest.getCreatedAt() != null
                            && certification.getCreatedAt().isAfter(latest.getCreatedAt()))) {
                latest = certification;
            }
        }
        return latest;
    }

    private LicenseCertification requireCertification(UUID certificationId) {
        return certifications.findById(certificationId)
                .orElseThrow(() -> NotFoundException.forId("Certification", certificationId));
    }

    private void requireEmployee(UUID employeeId) {
        if (employees.findById(employeeId).isEmpty()) {
            throw NotFoundException.forId("Employee", employeeId);
        }
    }
}
