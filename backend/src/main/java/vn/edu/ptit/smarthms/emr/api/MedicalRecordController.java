package vn.edu.ptit.smarthms.emr.api;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.AddendumRequest;
import vn.edu.ptit.smarthms.emr.api.EmrRequests.LockRequest;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.AddendumResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.RecordResponse;
import vn.edu.ptit.smarthms.emr.api.EmrResponses.RecordVersionResponse;
import vn.edu.ptit.smarthms.emr.service.MedicalRecordService;

/** Medical record lifecycle and addenda (FR-EMR-04). Demo-lock only: no legal signature. */
@RestController
@RequestMapping("/api/v1/emr")
public class MedicalRecordController {

    private final MedicalRecordService records;

    public MedicalRecordController(MedicalRecordService records) {
        this.records = records;
    }

    private Actor actor(Authentication authentication) {
        return ActorResolver.from(authentication);
    }

    @GetMapping("/encounters/{encounterId}/record")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION')")
    public RecordResponse getRecordByEncounter(Authentication authentication,
            @PathVariable UUID encounterId) {
        return records.getRecordByEncounter(actor(authentication), encounterId);
    }

    @PostMapping("/records/{recordId}/submit")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECORDS_MANAGER')")
    public RecordResponse submitForSignature(Authentication authentication,
            @PathVariable UUID recordId, @Valid @RequestBody LockRequest request) {
        return records.submitForSignature(actor(authentication), recordId, request);
    }

    @PostMapping("/records/{recordId}/lock")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECORDS_MANAGER')")
    public RecordResponse lockRecord(Authentication authentication, @PathVariable UUID recordId) {
        return records.lockRecord(actor(authentication), recordId);
    }

    @PostMapping("/records/{recordId}/archive")
    @PreAuthorize("hasRole('RECORDS_MANAGER')")
    public RecordResponse archiveRecord(Authentication authentication,
            @PathVariable UUID recordId) {
        return records.archiveRecord(actor(authentication), recordId);
    }

    @PostMapping("/records/{recordId}/addenda")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECORDS_MANAGER')")
    public ResponseEntity<AddendumResponse> addAddendum(Authentication authentication,
            @PathVariable UUID recordId, @Valid @RequestBody AddendumRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(records.addAddendum(actor(authentication), recordId, request));
    }

    @PostMapping("/addenda/{addendumId}/confirm")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECORDS_MANAGER')")
    public AddendumResponse confirmAddendum(Authentication authentication,
            @PathVariable UUID addendumId) {
        return records.confirmAddendum(actor(authentication), addendumId);
    }

    @GetMapping("/records/{recordId}/versions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION')")
    public List<RecordVersionResponse> listVersions(Authentication authentication,
            @PathVariable UUID recordId) {
        return records.listVersions(actor(authentication), recordId);
    }

    @GetMapping("/records/{recordId}/addenda")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'CLS_TECHNICIAN', 'RECORDS_MANAGER', 'RECEPTION')")
    public List<AddendumResponse> listAddenda(Authentication authentication,
            @PathVariable UUID recordId) {
        return records.listAddenda(actor(authentication), recordId);
    }
}
