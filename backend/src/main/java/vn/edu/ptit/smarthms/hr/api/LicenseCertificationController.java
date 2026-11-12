package vn.edu.ptit.smarthms.hr.api;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.hr.api.HrRequests.CreateCertificationRequest;
import vn.edu.ptit.smarthms.hr.api.HrRequests.UpdateCertificationRequest;
import vn.edu.ptit.smarthms.hr.api.HrResponses.CertificationResponse;
import vn.edu.ptit.smarthms.hr.service.LicenseCertificationService;

/** License and CCHN verification (FR-HR-02). */
@RestController
@RequestMapping("/api/v1/hr")
public class LicenseCertificationController {

    private final LicenseCertificationService certifications;

    public LicenseCertificationController(LicenseCertificationService certifications) {
        this.certifications = certifications;
    }

    private Actor actor(Authentication authentication) {
        return ActorResolver.from(authentication);
    }

    @PostMapping("/employees/{employeeId}/certifications")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<CertificationResponse> addCertification(Authentication authentication,
            @PathVariable UUID employeeId, @Valid @RequestBody CreateCertificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(certifications.addCertification(actor(authentication), employeeId, request));
    }

    @GetMapping("/employees/{employeeId}/certifications")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER', 'ADMIN')")
    public List<CertificationResponse> listCertifications(Authentication authentication,
            @PathVariable UUID employeeId) {
        return certifications.listCertifications(actor(authentication), employeeId);
    }

    @GetMapping("/certifications/{certificationId}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER', 'ADMIN')")
    public CertificationResponse getCertification(Authentication authentication,
            @PathVariable UUID certificationId) {
        return certifications.getCertification(actor(authentication), certificationId);
    }

    @PutMapping("/certifications/{certificationId}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public CertificationResponse updateCertification(Authentication authentication,
            @PathVariable UUID certificationId,
            @Valid @RequestBody UpdateCertificationRequest request) {
        return certifications.updateCertification(actor(authentication), certificationId, request);
    }

  @DeleteMapping("/certifications/{certificationId}")
  @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
  public ResponseEntity<Void> deleteCertification(Authentication authentication, @PathVariable UUID certificationId) {
    certifications.deleteCertification(actor(authentication), certificationId);
    return ResponseEntity.noContent().build();
  }
}
