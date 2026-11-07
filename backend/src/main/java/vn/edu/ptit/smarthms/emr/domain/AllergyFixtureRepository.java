package vn.edu.ptit.smarthms.emr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllergyFixtureRepository extends JpaRepository<AllergyFixture, UUID> {
    List<AllergyFixture> findByPatientId(UUID patientId);
    List<AllergyFixture> findByPatientIdAndAllergen(UUID patientId, String allergen);
}
