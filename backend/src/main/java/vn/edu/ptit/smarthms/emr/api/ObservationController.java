package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.smarthms.emr.domain.ClinicalObservation;
import vn.edu.ptit.smarthms.emr.service.ObservationService;

@RestController
@RequestMapping("/api/v1/emr/encounters/{encounterId}/observations")
public class ObservationController {
    public record CreateRequest(
            @NotBlank @Size(max = 60) String metricCode,
            @Digits(integer = 10, fraction = 4) BigDecimal numericValue,
            @Size(max = 2000) String textValue,
            @Size(max = 40) String unitCode,
            OffsetDateTime observedAt,
            ClinicalObservation.SourceType sourceType,
            @Size(max = 160) String sourceRef,
            ClinicalObservation.Quality quality) {}

    public record ObservationResponse(UUID observationId, UUID encounterId, String metricCode,
            BigDecimal numericValue, String textValue, String unitCode, OffsetDateTime observedAt,
            String sourceType, String sourceRef, String quality) {
        static ObservationResponse from(ClinicalObservation o) {
            return new ObservationResponse(o.getObservationId(), o.getEncounterId(), o.getMetricCode(),
                    o.getNumericValue(), o.getTextValue(), o.getUnitCode(), o.getObservedAt(),
                    o.getSourceType().name(), o.getSourceRef(), o.getQuality().name());
        }
    }

    private final ObservationService service;

    public ObservationController(ObservationService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ObservationResponse create(@PathVariable UUID encounterId, @Valid @RequestBody CreateRequest r) {
        return ObservationResponse.from(service.create(encounterId, r.metricCode(), r.numericValue(),
                r.textValue(), r.unitCode(), r.observedAt(), r.sourceType(), r.sourceRef(), r.quality()));
    }

    @GetMapping
    public List<ObservationResponse> list(@PathVariable UUID encounterId) {
        return service.list(encounterId).stream().map(ObservationResponse::from).toList();
    }
}