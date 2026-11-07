package vn.edu.ptit.smarthms.emr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalResultRepository extends JpaRepository<ClinicalResult, UUID> {
    List<ClinicalResult> findByOrderId(UUID orderId);
    Optional<ClinicalResult> findBySourceSystemAndSourceResultIdAndSourceVersion(
            String sourceSystem, String sourceResultId, String sourceVersion);
}
