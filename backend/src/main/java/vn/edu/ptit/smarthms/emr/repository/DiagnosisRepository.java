package vn.edu.ptit.smarthms.emr.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.Diagnosis;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisType;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {
    List<Diagnosis> findByEncounterIdOrderByCreatedAtAsc(UUID encounterId);
    boolean existsByEncounterIdAndDiagnosisType(UUID encounterId, DiagnosisType type);
}