package vn.edu.ptit.smarthms.emr.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.ClinicalObservation;

public interface ClinicalObservationRepository extends JpaRepository<ClinicalObservation, UUID> {
    List<ClinicalObservation> findByEncounterIdOrderByObservedAtAsc(UUID encounterId);
}