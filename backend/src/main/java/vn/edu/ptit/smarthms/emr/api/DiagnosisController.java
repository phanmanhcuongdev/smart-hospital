package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.smarthms.emr.service.DiagnosisService;

@RestController
@RequestMapping("/api/v1/emr/encounters/{encounterId}/diagnoses")
public class DiagnosisController {
    private final DiagnosisService service;

    public DiagnosisController(DiagnosisService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosisResponse create(@PathVariable UUID encounterId,
                                    @Valid @RequestBody CreateDiagnosisRequest req) {
        return DiagnosisResponse.from(service.create(
                encounterId, req.diagnosisType(), req.icd10Code(), req.narrative()));
    }

    @GetMapping
    public List<DiagnosisResponse> list(@PathVariable UUID encounterId) {
        return service.list(encounterId).stream().map(DiagnosisResponse::from).toList();
    }

    @PatchMapping("/{diagnosisId}/confirm")
    public DiagnosisResponse confirm(@PathVariable UUID encounterId, @PathVariable UUID diagnosisId) {
        return DiagnosisResponse.from(service.confirm(encounterId, diagnosisId));
    }
}