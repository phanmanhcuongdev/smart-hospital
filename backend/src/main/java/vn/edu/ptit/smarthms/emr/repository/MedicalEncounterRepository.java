package vn.edu.ptit.smarthms.emr.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;

public interface MedicalEncounterRepository extends JpaRepository<MedicalEncounter, UUID> {}