/**
 * Mô tả: Truy vấn hồ sơ theo lần khám, có bản khóa dòng để hai yêu cầu đồng thời không ghi đè nhau.
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 08/11/2026 (cập nhật 10/11/2026)
 */
package vn.edu.ptit.smarthms.emr.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {
    Optional<MedicalRecord> findByEncounterId(UUID encounterId);
}