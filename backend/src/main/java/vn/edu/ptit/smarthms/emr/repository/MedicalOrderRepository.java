/**
 * Mô tả: Truy vấn chỉ định cận lâm sàng theo lần khám.
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 08/11/2026
 */
package vn.edu.ptit.smarthms.emr.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrder;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, UUID> {
    List<MedicalOrder> findByEncounterIdOrderByCreatedAtAsc(UUID encounterId);
}