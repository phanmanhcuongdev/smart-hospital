package vn.edu.ptit.smarthms.emr.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, UUID> {
    Optional<MedicalRecord> findByEncounterId(UUID encounterId);

    /** Khóa dòng để hai yêu cầu đồng thời không ghi đè nhau (so sánh expectedVersion an toàn). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from MedicalRecord r where r.encounterId = :encounterId")
    Optional<MedicalRecord> findForUpdateByEncounterId(@Param("encounterId") UUID encounterId);
}