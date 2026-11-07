package vn.edu.ptit.smarthms.emr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {
    List<Diagnosis> findByEncounterId(UUID encounterId);
    Optional<Diagnosis> findByEncounterIdAndType(UUID encounterId, Diagnosis.Type type);
}
