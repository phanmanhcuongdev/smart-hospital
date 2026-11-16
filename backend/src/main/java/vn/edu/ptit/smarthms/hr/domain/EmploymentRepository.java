/**
 * Mô tả: Repository JPA truy vấn dữ liệu quá trình công tác của nhân viên
 * Người thực hiện: Tran Hoang Nam
 * Ngày thực hiện: 17/10/2026
 */

package vn.edu.ptit.smarthms.hr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentRepository extends JpaRepository<Employment, UUID> {
    List<Employment> findByEmployeeId(UUID employeeId);
    List<Employment> findByEmployeeIdAndEndAtIsNull(UUID employeeId);
    List<Employment> findByDepartmentId(UUID departmentId);
}
