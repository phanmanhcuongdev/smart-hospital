package vn.edu.ptit.smarthms.emr.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {
    Optional<MedicalRecord> findByEncounterId(UUID encounterId);
}