package vn.edu.ptit.smarthms.emr.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;

public interface MedicalEncounterRepository extends JpaRepository<MedicalEncounter, UUID> {
    Page<MedicalEncounter> findByPatientId(UUID patientId, Pageable pageable);
    Page<MedicalEncounter> findByStatus(EncounterStatus status, Pageable pageable);
    Page<MedicalEncounter> findByPatientIdAndStatus(UUID patientId, EncounterStatus status, Pageable pageable);
    Page<MedicalEncounter> findByDepartmentId(UUID departmentId, Pageable pageable);
    Page<MedicalEncounter> findByDepartmentIdAndStatus(UUID departmentId, EncounterStatus status, Pageable pageable);
}
