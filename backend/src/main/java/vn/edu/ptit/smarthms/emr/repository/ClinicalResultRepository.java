/**
 * Mô tả: Lưu và đọc kết quả cận lâm sàng giả lập; chống trùng bằng ràng buộc duy nhất (nguồn, mã kết quả, phiên bản).
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 10/11/2026
 */
package vn.edu.ptit.smarthms.emr.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.smarthms.emr.domain.ClinicalResult;

/**
 * Mô tả: Repository tra cứu kết quả cận lâm sàng theo chỉ định hoặc hệ thống nguồn và phiên bản
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
public interface ClinicalResultRepository extends JpaRepository<ClinicalResult, UUID> {
    Optional<ClinicalResult> findBySourceSystemAndSourceResultIdAndSourceVersion(
            String sourceSystem, String sourceResultId, String sourceVersion);

    List<ClinicalResult> findByOrderId(UUID orderId);

    List<ClinicalResult> findByQuality(String quality);

    long countByQuality(String quality);

    long countByOrderId(UUID orderId);
}
