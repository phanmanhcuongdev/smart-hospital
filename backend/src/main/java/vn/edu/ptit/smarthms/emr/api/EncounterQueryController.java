package vn.edu.ptit.smarthms.emr.api;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.service.EncounterQueryService;

@RestController
@RequestMapping("/api/v1/emr/encounters")
public class EncounterQueryController {
    public record PageResponse(List<EncounterResponse> items, int page, int size, long total) {}

    private final EncounterQueryService service;

    public EncounterQueryController(EncounterQueryService service) { this.service = service; }

    @GetMapping
    public PageResponse search(@RequestParam(required = false) UUID patientId,
                               @RequestParam(required = false) EncounterStatus status,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        
        var result = service.search(patientId, status, page, size);
        return new PageResponse(result.getContent().stream().map(EncounterResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }
}