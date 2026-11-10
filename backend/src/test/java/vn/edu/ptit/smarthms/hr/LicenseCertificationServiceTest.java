package vn.edu.ptit.smarthms.hr;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.hr.api.HrRequests;
import vn.edu.ptit.smarthms.hr.domain.Employee;
import vn.edu.ptit.smarthms.hr.domain.EmployeeRepository;
import vn.edu.ptit.smarthms.hr.domain.LicenseCertification;
import vn.edu.ptit.smarthms.hr.domain.LicenseCertificationRepository;
import vn.edu.ptit.smarthms.hr.service.LicenseCertificationService;

@ExtendWith(MockitoExtension.class)
class LicenseCertificationServiceTest {

    @Mock
    private LicenseCertificationRepository certifications;

    @Mock
    private EmployeeRepository employees;

    @InjectMocks
    private LicenseCertificationService service;

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 7);

    private static final UUID EMPLOYEE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static LicenseCertification cert(LicenseCertification.VerificationStatus status, LocalDate validUntil) {
        LicenseCertification certification = new LicenseCertification(EMPLOYEE_ID, "CCHN", "SRC-1");
        certification.setVerificationStatus(status);
        certification.setValidFrom(TODAY.minusYears(1));
        certification.setValidUntil(validUntil);
        return certification;
    }

    @Test
    void verifiedWithoutExpiryIsValid() {
        assertEquals("VALID", LicenseCertificationService.computeValidity(cert(LicenseCertification.VerificationStatus.VERIFIED, null), TODAY));
    }

    @Test
    void expiringSoonBoundaries() {
        assertEquals("EXPIRING_SOON", LicenseCertificationService.computeValidity(cert(LicenseCertification.VerificationStatus.VERIFIED, TODAY), TODAY));
        assertEquals("EXPIRING_SOON", LicenseCertificationService.computeValidity(cert(LicenseCertification.VerificationStatus.VERIFIED, TODAY.plusDays(1)), TODAY));
        assertEquals("EXPIRING_SOON", LicenseCertificationService.computeValidity(cert(LicenseCertification.VerificationStatus.VERIFIED, TODAY.plusDays(30)), TODAY));
        assertEquals("VALID", LicenseCertificationService.computeValidity(cert(LicenseCertification.VerificationStatus.VERIFIED, TODAY.plusDays(31)), TODAY));
    }

    @Test
    void expiredOnDayAfterExpiry() {
        assertEquals("EXPIRED", LicenseCertificationService.computeValidity(cert(LicenseCertification.VerificationStatus.VERIFIED, TODAY.minusDays(1)), TODAY));
    }

    @Test
    void unverifiedIsNeverEligible() {
        assertEquals("UNVERIFIED", LicenseCertificationService.computeValidity(cert(LicenseCertification.VerificationStatus.UNVERIFIED, TODAY.plusDays(10)), TODAY));
    }

    @Test
    void rejectedAndRevokedMapToRejected() {
        assertEquals("REJECTED", LicenseCertificationService.computeValidity(cert(LicenseCertification.VerificationStatus.REJECTED, TODAY.plusDays(10)), TODAY));
        assertEquals("REJECTED", LicenseCertificationService.computeValidity(cert(LicenseCertification.VerificationStatus.REVOKED, TODAY.plusDays(10)), TODAY));
    }

    @Test
    void hasValidCchnAcceptsExpiringSoon() {
        when(certifications.findByEmployeeIdAndCredentialType(EMPLOYEE_ID, "CCHN"))
                .thenReturn(List.of(cert(LicenseCertification.VerificationStatus.VERIFIED, TODAY.plusDays(10))));
        assertTrue(service.hasValidCchn(EMPLOYEE_ID, TODAY));
        assertEquals("EXPIRING_SOON", service.cchnValidityFor(EMPLOYEE_ID, TODAY));
    }

    @Test
    void hasValidCchnRejectsExpiredAndUnverified() {
        when(certifications.findByEmployeeIdAndCredentialType(EMPLOYEE_ID, "CCHN"))
                .thenReturn(List.of(cert(LicenseCertification.VerificationStatus.VERIFIED, TODAY.minusDays(1))));
        assertFalse(service.hasValidCchn(EMPLOYEE_ID, TODAY));
        assertEquals("EXPIRED", service.cchnValidityFor(EMPLOYEE_ID, TODAY));
        when(certifications.findByEmployeeIdAndCredentialType(EMPLOYEE_ID, "CCHN"))
                .thenReturn(List.of(cert(LicenseCertification.VerificationStatus.UNVERIFIED, TODAY.plusDays(90))));
        assertFalse(service.hasValidCchn(EMPLOYEE_ID, TODAY));
    }

    @Test
    void hasValidCchnWithNoCertificationIsFalse() {
        when(certifications.findByEmployeeIdAndCredentialType(EMPLOYEE_ID, "CCHN")).thenReturn(List.of());
        assertFalse(service.hasValidCchn(EMPLOYEE_ID, TODAY));
        assertEquals("UNVERIFIED", service.cchnValidityFor(EMPLOYEE_ID, TODAY));
    }

    @Test
    void addCertificationRejectsExpiryBeforeIssue() {
        Employee employee = new Employee("EMP-T-100", "Test Person");
        when(employees.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        HrRequests.CreateCertificationRequest request = new HrRequests.CreateCertificationRequest(
                "CCHN", "LIC-1", "Authority", null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2025, 12, 31),
                LicenseCertification.VerificationStatus.UNVERIFIED, "SRC-1");
        Actor actor = new Actor("demo-hr", Set.of("HR_SPECIALIST"));
        assertThrows(ValidationException.class, () -> service.addCertification(actor, EMPLOYEE_ID, request));
        verify(certifications, never()).save(any());
    }
}
