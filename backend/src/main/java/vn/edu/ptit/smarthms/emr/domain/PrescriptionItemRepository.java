package vn.edu.ptit.smarthms.emr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, PrescriptionItem.Key> {
    List<PrescriptionItem> findByIdPrescriptionId(UUID prescriptionId);
}
