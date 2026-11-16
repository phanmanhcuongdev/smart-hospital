/**
 * Mô tả: Repository JPA truy vấn dữ liệu chứng chỉ của nhân viên
 * Người thực hiện: Tran Hoang Nam
 * Ngày thực hiện: 18/10/2026
 */

package vn.edu.ptit.smarthms.hr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseCertificationRepository
        extends JpaRepository<LicenseCertification, UUID> {
    List<LicenseCertification> findByEmployeeId(UUID employeeId);
    List<LicenseCertification> findByEmployeeIdAndCredentialType(UUID employeeId,
            String credentialType);
}
