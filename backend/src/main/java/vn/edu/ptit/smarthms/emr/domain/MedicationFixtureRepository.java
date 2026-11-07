package vn.edu.ptit.smarthms.emr.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationFixtureRepository extends JpaRepository<MedicationFixture, UUID> {
}
