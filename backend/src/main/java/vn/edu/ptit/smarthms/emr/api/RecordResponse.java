/**
 * Mô tả: Dữ liệu trả về của hồ sơ bệnh án, gồm trạng thái, phiên bản và mã kiểm soát thay đổi.
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 08/11/2026 (cập nhật 10/11/2026)
 */
package vn.edu.ptit.smarthms.emr.api;

import java.time.OffsetDateTime;
import java.util.UUID;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;

public record RecordResponse(UUID recordId, UUID encounterId, String recordStatus,
                             int currentVersion, OffsetDateTime lockedAt, OffsetDateTime createdAt) {
    public static RecordResponse from(MedicalRecord r) {
        return new RecordResponse(r.getRecordId(), r.getEncounterId(), r.getRecordStatus().name(),
                r.getCurrentVersion(), r.getLockedAt(), r.getCreatedAt());
    }
}