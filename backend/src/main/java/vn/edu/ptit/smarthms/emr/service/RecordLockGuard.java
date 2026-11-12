package vn.edu.ptit.smarthms.emr.service;

import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.emr.domain.RecordStatus;
import vn.edu.ptit.smarthms.emr.repository.MedicalRecordRepository;

/** Chặn sửa dữ liệu gốc khi hồ sơ đã nộp/khóa. Giả định, cần đối chiếu AC/IV.7. */
@Component
public class RecordLockGuard {
    private static final Set<RecordStatus> FROZEN =
            Set.of(RecordStatus.PENDING_SIGNATURE, RecordStatus.LOCKED, RecordStatus.ARCHIVED);

    private final MedicalRecordRepository records;

    public RecordLockGuard(MedicalRecordRepository records) { this.records = records; }

    public void assertEditable(UUID encounterId) {
        records.findByEncounterId(encounterId)
                .filter(r -> FROZEN.contains(r.getRecordStatus()))
                .ifPresent(r -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Medical record is " + r.getRecordStatus() + "; changes must use an addendum");
                });
    }
}