package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.emr.domain.Prescription;
import vn.edu.ptit.smarthms.emr.domain.PrescriptionItem;
import vn.edu.ptit.smarthms.emr.service.PrescriptionService;

@RestController
@RequestMapping("/api/v1/emr/encounters/{encounterId}/prescriptions")
public class PrescriptionController {
    public record AddItemRequest(
            @NotNull UUID medicationId,
            @NotBlank @Size(max = 100) String dose,
            @NotBlank @Size(max = 80) String route,
            @NotBlank @Size(max = 120) String frequency,
            @NotNull @Min(1) Integer daysCount,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 3)
            BigDecimal quantity,
            @NotBlank @Size(max = 40) String unitCode) {}

    public record ItemResponse(int lineNo, UUID medicationId, String medicationLabel, String dose,
                               String route, String frequency, int daysCount, BigDecimal quantity,
                               String unitCode) {
        static ItemResponse from(PrescriptionItem i) {
            return new ItemResponse(i.getLineNo(), i.getMedicationId(), i.getMedicationLabel(),
                    i.getDose(), i.getRoute(), i.getFrequency(), i.getDaysCount(),
                    i.getQuantity(), i.getUnitCode());
        }
    }

    public record PrescriptionResponse(UUID prescriptionId, UUID encounterId, UUID prescriberId,
                                        String status, OffsetDateTime issuedAt, List<ItemResponse> items) {}

    private final PrescriptionService service;

    public PrescriptionController(PrescriptionService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public PrescriptionResponse create(Authentication authentication, @PathVariable UUID encounterId) {
        return view(service.create(ActorResolver.from(authentication), encounterId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'RECORDS_MANAGER', 'ADMIN')")
    public List<PrescriptionResponse> list(@PathVariable UUID encounterId) {
        return service.list(encounterId).stream().map(this::view).toList();
    }

    @GetMapping("/{prescriptionId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'RECORDS_MANAGER', 'ADMIN')")
    public PrescriptionResponse get(@PathVariable UUID encounterId, @PathVariable UUID prescriptionId) {
        return view(service.get(encounterId, prescriptionId));
    }

    @PostMapping("/{prescriptionId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public PrescriptionResponse addItem(Authentication authentication,
                                        @PathVariable UUID encounterId,
                                        @PathVariable UUID prescriptionId,
                                        @Valid @RequestBody AddItemRequest r) {
        return view(service.addItem(ActorResolver.from(authentication), encounterId, prescriptionId, r.medicationId(), r.dose(), r.route(),
                r.frequency(), r.daysCount(), r.quantity(), r.unitCode()));
    }

    @PostMapping("/{prescriptionId}/issue")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public PrescriptionResponse issue(Authentication authentication,
                                      @PathVariable UUID encounterId,
                                      @PathVariable UUID prescriptionId) {
        return view(service.issue(ActorResolver.from(authentication), encounterId, prescriptionId));
    }

    @PostMapping("/{prescriptionId}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public PrescriptionResponse cancel(Authentication authentication,
                                       @PathVariable UUID encounterId,
                                       @PathVariable UUID prescriptionId) {
        return view(service.cancel(ActorResolver.from(authentication), encounterId, prescriptionId));
    }

    private PrescriptionResponse view(Prescription p) {
        List<ItemResponse> lines = service.itemsOf(p.getPrescriptionId()).stream()
                .map(ItemResponse::from).toList();
        return new PrescriptionResponse(p.getPrescriptionId(), p.getEncounterId(), p.getPrescriberId(),
                p.getStatus().name(), p.getIssuedAt(), lines);
    }
}
