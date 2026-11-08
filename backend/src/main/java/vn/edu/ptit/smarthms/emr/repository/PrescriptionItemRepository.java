package vn.edu.ptit.smarthms.emr.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.PrescriptionItem;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, PrescriptionItem.Key> {
    List<PrescriptionItem> findByIdPrescriptionIdOrderByIdLineNoAsc(UUID prescriptionId);
    long countByIdPrescriptionId(UUID prescriptionId);
}