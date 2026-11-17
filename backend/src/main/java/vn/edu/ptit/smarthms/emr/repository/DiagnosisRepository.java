/**
 * Mô tả: Truy vấn chẩn đoán theo lần khám và kiểm tra đã có chẩn đoán chính được xác nhận chưa.
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 08/11/2026 (cập nhật 09/11/2026)
 */
package vn.edu.ptit.smarthms.emr.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.Diagnosis;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisStatus;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisType;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {
    List<Diagnosis> findByEncounterIdOrderByCreatedAtAsc(UUID encounterId);
    boolean existsByEncounterIdAndDiagnosisType(UUID encounterId, DiagnosisType type);
    boolean existsByEncounterIdAndDiagnosisTypeAndDiagnosisStatus(
            UUID encounterId, DiagnosisType type, DiagnosisStatus status);
}