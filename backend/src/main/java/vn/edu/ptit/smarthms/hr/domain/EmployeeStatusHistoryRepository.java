/**
 * Mô tả: Repository JPA truy vấn lịch sử thay đổi trạng thái của nhân viên
 * Người thực hiện: Tran Hoang Nam
 * Ngày thực hiện: 19/10/2026
 */

package vn.edu.ptit.smarthms.hr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeStatusHistoryRepository
        extends JpaRepository<EmployeeStatusHistory, UUID> {
    List<EmployeeStatusHistory> findByEmployeeIdOrderByEffectiveAtAsc(UUID employeeId);
}
