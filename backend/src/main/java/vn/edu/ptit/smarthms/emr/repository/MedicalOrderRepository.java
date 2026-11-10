package vn.edu.ptit.smarthms.emr.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrder;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, UUID> {
    List<MedicalOrder> findByEncounterIdOrderByCreatedAtAsc(UUID encounterId);
}