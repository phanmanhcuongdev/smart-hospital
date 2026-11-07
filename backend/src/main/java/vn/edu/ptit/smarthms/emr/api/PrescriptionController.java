package vn.edu.ptit.smarthms.emr.api;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.PrescriptionRequest;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.PrescriptionResponse;
import vn.edu.ptit.smarthms.emr.service.PrescriptionService;

/** Prescriptions (FR-EMR-03). Prescribed is not dispensed. */
@RestController
@RequestMapping("/api/v1/emr")
public class PrescriptionController {

    private final PrescriptionService prescriptions;

    public PrescriptionController(PrescriptionService prescriptions) {
        this.prescriptions = prescriptions;
    }

    private Actor actor(Authentication authentication) {
        return ActorResolver.from(authentication);
    }

    @PostMapping("/encounters/{encounterId}/prescriptions")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<PrescriptionResponse> createPrescription(Authentication authentication,
            @PathVariable UUID encounterId, @Valid @RequestBody PrescriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(prescriptions.createPrescription(actor(authentication), encounterId, request));
    }

    @GetMapping("/encounters/{encounterId}/prescriptions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION')")
    public List<PrescriptionResponse> listByEncounter(Authentication authentication,
            @PathVariable UUID encounterId) {
        return prescriptions.listByEncounter(actor(authentication), encounterId);
    }

    @GetMapping("/prescriptions/{prescriptionId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION')")
    public PrescriptionResponse getPrescription(Authentication authentication,
            @PathVariable UUID prescriptionId) {
        return prescriptions.getPrescription(actor(authentication), prescriptionId);
    }

    @PostMapping("/prescriptions/{prescriptionId}/issue")
    @PreAuthorize("hasRole('DOCTOR')")
    public PrescriptionResponse issuePrescription(Authentication authentication,
            @PathVariable UUID prescriptionId) {
        return prescriptions.issuePrescription(actor(authentication), prescriptionId);
    }

    @PostMapping("/prescriptions/{prescriptionId}/cancel")
    @PreAuthorize("hasRole('DOCTOR')")
    public PrescriptionResponse cancelPrescription(Authentication authentication,
            @PathVariable UUID prescriptionId) {
        return prescriptions.cancelPrescription(actor(authentication), prescriptionId);
    }
}
