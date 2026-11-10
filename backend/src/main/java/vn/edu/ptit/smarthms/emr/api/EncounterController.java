package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.smarthms.emr.service.EncounterService;

@RestController
@RequestMapping("/api/v1/emr/encounters")
public class EncounterController {
    private final EncounterService service;

    public EncounterController(EncounterService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EncounterResponse create(@Valid @RequestBody CreateEncounterRequest req) {
        return EncounterResponse.from(
                service.create(req.patientId(), req.appointmentId(), req.attendingEmployeeId()));
    }

    @GetMapping("/{id}")
    public EncounterResponse get(@PathVariable UUID id) {
        return EncounterResponse.from(service.get(id));
    }

    @PatchMapping("/{id}/status")
    public EncounterResponse changeStatus(@PathVariable UUID id,
                                          @Valid @RequestBody UpdateStatusRequest req) {
        return EncounterResponse.from(service.changeStatus(id, req.status()));
    }

    @GetMapping("/{id}/record")
    public RecordResponse getRecord(@PathVariable UUID id) {
        return RecordResponse.from(service.getRecord(id));
    }
}