package vn.edu.ptit.smarthms.emr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalEncounterRepository extends JpaRepository<MedicalEncounter, UUID> {
    List<MedicalEncounter> findByPatientId(UUID patientId);
    List<MedicalEncounter> findByAttendingEmployeeId(UUID attendingEmployeeId);
    List<MedicalEncounter> findByStatus(MedicalEncounter.Status status);
}
