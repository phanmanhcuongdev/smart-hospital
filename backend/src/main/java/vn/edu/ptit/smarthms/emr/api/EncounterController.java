package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.emr.service.EncounterService;

@RestController
@RequestMapping("/api/v1/emr/encounters")
public class EncounterController {
    private final EncounterService service;

    public EncounterController(EncounterService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'RECEPTION', 'ADMIN')")
    public EncounterResponse create(Authentication authentication,
                                    @Valid @RequestBody CreateEncounterRequest req) {
        return EncounterResponse.from(
                service.create(ActorResolver.from(authentication), req.patientId(), req.appointmentId(),
                        req.attendingEmployeeId(), req.departmentId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION', 'ADMIN')")
    public EncounterResponse get(@PathVariable UUID id) {
        return EncounterResponse.from(service.get(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public EncounterResponse changeStatus(Authentication authentication,
                                          @PathVariable UUID id,
                                          @Valid @RequestBody UpdateStatusRequest req) {
        return EncounterResponse.from(service.changeStatus(
                ActorResolver.from(authentication), id, req.status(),
                req.attendingEmployeeId(), req.departmentId()));
    }

    @GetMapping("/{id}/record")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'RECORDS_MANAGER', 'ADMIN')")
    public RecordResponse getRecord(@PathVariable UUID id) {
        return RecordResponse.from(service.getRecord(id));
    }
}