/**
 * Mô tả: Truy vấn các dòng thuốc của một đơn và đếm số dòng.
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 08/11/2026
 */
package vn.edu.ptit.smarthms.emr.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.PrescriptionItem;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, PrescriptionItem.Key> {
    List<PrescriptionItem> findByIdPrescriptionIdOrderByIdLineNoAsc(UUID prescriptionId);
    long countByIdPrescriptionId(UUID prescriptionId);
}