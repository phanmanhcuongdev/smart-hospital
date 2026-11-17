/**
 * Mô tả: Truy vấn đơn thuốc theo lần khám.
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 08/11/2026
 */
package vn.edu.ptit.smarthms.emr.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.Prescription;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    List<Prescription> findByEncounterIdOrderByCreatedAtAsc(UUID encounterId);
}