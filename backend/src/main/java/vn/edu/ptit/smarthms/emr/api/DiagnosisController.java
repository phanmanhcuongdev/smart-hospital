package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.emr.service.DiagnosisService;

@RestController
@RequestMapping("/api/v1/emr/encounters/{encounterId}/diagnoses")
public class DiagnosisController {
    private final DiagnosisService service;

    public DiagnosisController(DiagnosisService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public DiagnosisResponse create(Authentication authentication,
                                    @PathVariable UUID encounterId,
                                    @Valid @RequestBody CreateDiagnosisRequest req) {
        return DiagnosisResponse.from(service.create(
                ActorResolver.from(authentication), encounterId, req.diagnosisType(), req.icd10Code(), req.narrative()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'RECORDS_MANAGER', 'ADMIN')")
    public List<DiagnosisResponse> list(@PathVariable UUID encounterId) {
        return service.list(encounterId).stream().map(DiagnosisResponse::from).toList();
    }

    @PatchMapping("/{diagnosisId}/confirm")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public DiagnosisResponse confirm(Authentication authentication,
                                     @PathVariable UUID encounterId,
                                     @PathVariable UUID diagnosisId) {
        return DiagnosisResponse.from(service.confirm(ActorResolver.from(authentication), encounterId, diagnosisId));
    }
}
