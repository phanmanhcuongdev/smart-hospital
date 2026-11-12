package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
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
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public VersionResponse snapshot(Authentication authentication, @PathVariable UUID encounterId) {
        return VersionResponse.from(service.snapshot(ActorResolver.from(authentication), encounterId));
    }

    @GetMapping("/versions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'RECORDS_MANAGER', 'ADMIN')")
    public List<VersionResponse> versions(@PathVariable UUID encounterId) {
        return service.listVersions(encounterId).stream().map(VersionResponse::from).toList();
    }

    @GetMapping("/versions/{versionNo}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'RECORDS_MANAGER', 'ADMIN')")
    public VersionResponse version(@PathVariable UUID encounterId, @PathVariable int versionNo) {
        return VersionResponse.from(service.getVersion(encounterId, versionNo));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public RecordResponse submit(Authentication authentication, @PathVariable UUID encounterId) {
        return RecordResponse.from(service.submit(ActorResolver.from(authentication), encounterId));
    }

    @PostMapping("/lock")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECORDS_MANAGER', 'ADMIN')")
    public RecordResponse lock(Authentication authentication, @PathVariable UUID encounterId, @Valid @RequestBody UserRequest req) {
        return RecordResponse.from(service.lock(ActorResolver.from(authentication), encounterId, req.userId()));
    }

    @PostMapping("/addenda")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public AddendumResponse createAddendum(Authentication authentication,
                                           @PathVariable UUID encounterId,
                                           @Valid @RequestBody AddendumRequest req) {
        return AddendumResponse.from(service.createAddendum(ActorResolver.from(authentication), encounterId, req.reason(), req.content()));
    }

    @GetMapping("/addenda")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'RECORDS_MANAGER', 'ADMIN')")
    public List<AddendumResponse> addenda(@PathVariable UUID encounterId) {
        return service.listAddenda(encounterId).stream().map(AddendumResponse::from).toList();
    }

    @PostMapping("/addenda/{addendumId}/confirm")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECORDS_MANAGER', 'ADMIN')")
    public AddendumResponse confirmAddendum(Authentication authentication,
                                            @PathVariable UUID encounterId,
                                            @PathVariable UUID addendumId,
                                            @Valid @RequestBody UserRequest req) {
        return AddendumResponse.from(service.confirmAddendum(ActorResolver.from(authentication), encounterId, addendumId, req.userId()));
    }
}
