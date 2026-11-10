package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.smarthms.emr.repository.ClinicalResultRepository.ResultRow;
import vn.edu.ptit.smarthms.emr.service.ResultService;
import vn.edu.ptit.smarthms.emr.service.ResultService.Disposition;
import vn.edu.ptit.smarthms.emr.service.ResultService.Outcome;

/** Cổng nhận kết quả CLS giả lập (IF-EMR-04). Không phải API HTTP của IV.4. */
@RestController
@RequestMapping("/api/v1/emr/orders/{orderId}/results")
public class ResultController {
    public record ResultRequest(
            @NotBlank @Size(max = 100) String sourceSystem,
            @NotBlank @Size(max = 160) String sourceResultId,
            @NotBlank @Size(max = 60) String sourceVersion,
            @NotNull UUID patientId,
            OffsetDateTime resultAt,
            @NotBlank @Size(max = 20000) String payload,
            Boolean abnormalFlag) {}

    public record ResultResponse(UUID resultId, UUID orderId, String sourceSystem, String sourceResultId,
                                 String sourceVersion, OffsetDateTime receivedAt, OffsetDateTime resultAt,
                                 String payload, Boolean abnormalFlag, String quality) {
        static ResultResponse from(ResultRow r) {
            return new ResultResponse(r.resultId(), r.orderId(), r.sourceSystem(), r.sourceResultId(),
                    r.sourceVersion(), r.receivedAt(), r.resultAt(), r.payload(), r.abnormalFlag(), r.quality());
        }
    }

    public record IngestResponse(String disposition, ResultResponse result) {}

    private final ResultService service;

    public ResultController(ResultService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<IngestResponse> ingest(@PathVariable UUID orderId, @Valid @RequestBody ResultRequest r) {
        Outcome outcome = service.accept(orderId, r.sourceSystem(), r.sourceResultId(), r.sourceVersion(),
                r.patientId(), r.resultAt(), r.payload(), r.abnormalFlag());
        HttpStatus status = outcome.disposition() == Disposition.DUPLICATE ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status)
                .body(new IngestResponse(outcome.disposition().name(), ResultResponse.from(outcome.result())));
    }

    @GetMapping
    public List<ResultResponse> list(@PathVariable UUID orderId) {
        return service.list(orderId).stream().map(ResultResponse::from).toList();
    }
}