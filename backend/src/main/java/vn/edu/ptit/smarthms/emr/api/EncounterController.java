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
import vn.edu.ptit.smarthms.emr.api.EmrRequests.DiagnosisRequest;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.OpenEncounterRequest;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.VitalsRequest;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.DiagnosisResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.EncounterDetailResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.EncounterResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.ObservationResponse;
import vn.edu.ptit.smarthms.emr.service.EncounterService;

/** Encounters, vitals and diagnoses (FR-EMR-01). */
@RestController
@RequestMapping("/api/v1/emr/encounters")
public class EncounterController {

    private final EncounterService encounters;

    public EncounterController(EncounterService encounters) {
        this.encounters = encounters;
    }

    private Actor actor(Authentication authentication) {
        return ActorResolver.from(authentication);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTION')")
    public ResponseEntity<EncounterResponse> openEncounter(Authentication authentication,
            @Valid @RequestBody OpenEncounterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(encounters.openEncounter(actor(authentication), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION')")
    public List<EncounterResponse> listEncounters(Authentication authentication) {
        return encounters.listEncounters(actor(authentication));
    }

    @GetMapping("/{encounterId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION')")
    public EncounterResponse getEncounter(Authentication authentication,
            @PathVariable UUID encounterId) {
        return encounters.getEncounter(actor(authentication), encounterId);
    }

    @GetMapping("/{encounterId}/detail")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION')")
    public EncounterDetailResponse getDetail(Authentication authentication,
            @PathVariable UUID encounterId) {
        return encounters.getDetail(actor(authentication), encounterId);
    }

    @PostMapping("/{encounterId}/start")
    @PreAuthorize("hasRole('DOCTOR')")
    public EncounterResponse startEncounter(Authentication authentication,
            @PathVariable UUID encounterId) {
        return encounters.startEncounter(actor(authentication), encounterId);
    }

    @PostMapping("/{encounterId}/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public EncounterResponse completeEncounter(Authentication authentication,
            @PathVariable UUID encounterId) {
        return encounters.completeEncounter(actor(authentication), encounterId);
    }

    @PostMapping("/{encounterId}/cancel")
    @PreAuthorize("hasRole('DOCTOR')")
    public EncounterResponse cancelEncounter(Authentication authentication,
            @PathVariable UUID encounterId) {
        return encounters.cancelEncounter(actor(authentication), encounterId);
    }

    @PostMapping("/{encounterId}/vitals")
    @PreAuthorize("hasRole('NURSE')")
    public ResponseEntity<ObservationResponse> recordVitals(Authentication authentication,
            @PathVariable UUID encounterId, @Valid @RequestBody VitalsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(encounters.recordVitals(actor(authentication), encounterId, request));
    }

    @GetMapping("/{encounterId}/vitals")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION')")
    public List<ObservationResponse> listVitals(Authentication authentication,
            @PathVariable UUID encounterId) {
        return encounters.listVitals(actor(authentication), encounterId);
    }

    @PostMapping("/{encounterId}/diagnoses")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DiagnosisResponse> recordDiagnosis(Authentication authentication,
            @PathVariable UUID encounterId, @Valid @RequestBody DiagnosisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(encounters.recordDiagnosis(actor(authentication), encounterId, request));
    }

    @GetMapping("/{encounterId}/diagnoses")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION')")
    public List<DiagnosisResponse> listDiagnoses(Authentication authentication,
            @PathVariable UUID encounterId) {
        return encounters.listDiagnoses(actor(authentication), encounterId);
    }
}
