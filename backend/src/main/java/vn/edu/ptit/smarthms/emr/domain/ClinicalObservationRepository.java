package vn.edu.ptit.smarthms.emr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicalObservationRepository extends JpaRepository<ClinicalObservation, UUID> {
    List<ClinicalObservation> findByEncounterId(UUID encounterId);
}
