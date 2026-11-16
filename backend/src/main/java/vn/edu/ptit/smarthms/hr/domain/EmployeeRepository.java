/**
 * Mô tả: Repository JPA truy vấn dữ liệu nhân viên
 * Người thực hiện: Tran Hoang Nam
 * Ngày thực hiện: 17/10/2026
 */

package vn.edu.ptit.smarthms.hr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCode(String employeeCode);
    List<Employee> findByStatus(Employee.Status status);
}
