/**
 * Mô tả: Truy vấn sinh hiệu theo lần khám, sắp theo thời điểm đo.
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 08/11/2026
 */
package vn.edu.ptit.smarthms.emr.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.ClinicalObservation;

public interface ClinicalObservationRepository extends JpaRepository<ClinicalObservation, UUID> {
    List<ClinicalObservation> findByEncounterIdOrderByObservedAtAsc(UUID encounterId);
}