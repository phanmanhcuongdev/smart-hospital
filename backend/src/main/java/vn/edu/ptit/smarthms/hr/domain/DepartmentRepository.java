/**
 * Mô tả: Repository JPA truy vấn dữ liệu phòng ban
 * Người thực hiện: Tran Hoang Nam
 * Ngày thực hiện: 16/10/2026
 */

package vn.edu.ptit.smarthms.hr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByDepartmentCode(String departmentCode);
    boolean existsByDepartmentCode(String departmentCode);
    List<Department> findByActive(boolean active);
}
