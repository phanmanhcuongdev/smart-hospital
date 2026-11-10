package vn.edu.ptit.smarthms.emr.service;

import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.emr.domain.RecordStatus;
import vn.edu.ptit.smarthms.emr.port.AuditPort;
import vn.edu.ptit.smarthms.emr.repository.MedicalRecordRepository;

/** Chặn sửa dữ liệu gốc khi hồ sơ đã nộp/khóa và ghi dấu vết từ chối (IV7 V-09). */
@Component
public class RecordLockGuard {
    private static final Set<RecordStatus> FROZEN =
            Set.of(RecordStatus.PENDING_SIGNATURE, RecordStatus.LOCKED, RecordStatus.ARCHIVED);

    private final MedicalRecordRepository records;
    private final AuditPort audit;

    public RecordLockGuard(MedicalRecordRepository records, AuditPort audit) {
        this.records = records;
        this.audit = audit;
    }

    public void assertEditable(UUID encounterId) {
        records.findByEncounterId(encounterId)
                .filter(r -> FROZEN.contains(r.getRecordStatus()))
                .ifPresent(r -> {
                    audit.denied("RECORD_MODIFICATION_DENIED", "MEDICAL_RECORD", r.getRecordId());
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Medical record is " + r.getRecordStatus() + "; changes must use an addendum");
                });
    }
}