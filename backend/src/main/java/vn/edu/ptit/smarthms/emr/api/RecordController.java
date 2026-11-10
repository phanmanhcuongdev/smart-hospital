package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.smarthms.emr.domain.Addendum;
import vn.edu.ptit.smarthms.emr.repository.RecordSnapshotRepository.VersionRow;
import vn.edu.ptit.smarthms.emr.service.RecordService;

@RestController
@RequestMapping("/api/v1/emr/encounters/{encounterId}/record")
public class RecordController {
    /** userId tạm thời do client gửi; V.2 thay bằng người dùng đăng nhập. */
    public record UserRequest(@NotNull UUID userId) {}

    public record AddendumRequest(@NotBlank @Size(max = 2000) String reason,
                                  @NotBlank @Size(max = 4000) String content) {}

    public record VersionResponse(int versionNo, String sha256Hex, OffsetDateTime versionCreatedAt,
                                  String payload) {
        static VersionResponse from(VersionRow v) {
            return new VersionResponse(v.versionNo(), v.sha256Hex(), v.versionCreatedAt(), v.payload());
        }
    }

    public record AddendumResponse(UUID addendumId, UUID recordId, String reason, String content,
                                   String confirmationStatus, OffsetDateTime confirmedAt,
                                   UUID confirmedBy, OffsetDateTime createdAt) {
        static AddendumResponse from(Addendum a) {
            return new AddendumResponse(a.getAddendumId(), a.getRecordId(), a.getReason(), a.getContent(),
                    a.getConfirmationStatus().name(), a.getConfirmedAt(), a.getConfirmedBy(), a.getCreatedAt());
        }
    }

    private final RecordService service;

    public RecordController(RecordService service) { this.service = service; }

    @PostMapping("/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public VersionResponse snapshot(@PathVariable UUID encounterId) {
        return VersionResponse.from(service.snapshot(encounterId));
    }

    @GetMapping("/versions")
    public List<VersionResponse> versions(@PathVariable UUID encounterId) {
        return service.listVersions(encounterId).stream().map(VersionResponse::from).toList();
    }

    @GetMapping("/versions/{versionNo}")
    public VersionResponse version(@PathVariable UUID encounterId, @PathVariable int versionNo) {
        return VersionResponse.from(service.getVersion(encounterId, versionNo));
    }

    @PostMapping("/submit")
    public RecordResponse submit(@PathVariable UUID encounterId) {
        return RecordResponse.from(service.submit(encounterId));
    }

    @PostMapping("/lock")
    public RecordResponse lock(@PathVariable UUID encounterId, @Valid @RequestBody UserRequest req) {
        return RecordResponse.from(service.lock(encounterId, req.userId()));
    }

    @PostMapping("/addenda")
    @ResponseStatus(HttpStatus.CREATED)
    public AddendumResponse createAddendum(@PathVariable UUID encounterId,
                                           @Valid @RequestBody AddendumRequest req) {
        return AddendumResponse.from(service.createAddendum(encounterId, req.reason(), req.content()));
    }

    @GetMapping("/addenda")
    public List<AddendumResponse> addenda(@PathVariable UUID encounterId) {
        return service.listAddenda(encounterId).stream().map(AddendumResponse::from).toList();
    }

    @PostMapping("/addenda/{addendumId}/confirm")
    public AddendumResponse confirmAddendum(@PathVariable UUID encounterId, @PathVariable UUID addendumId,
                                            @Valid @RequestBody UserRequest req) {
        return AddendumResponse.from(service.confirmAddendum(encounterId, addendumId, req.userId()));
    }
}