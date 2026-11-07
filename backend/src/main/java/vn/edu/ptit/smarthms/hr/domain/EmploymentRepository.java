package vn.edu.ptit.smarthms.hr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentRepository extends JpaRepository<Employment, UUID> {
    List<Employment> findByEmployeeId(UUID employeeId);
    List<Employment> findByEmployeeIdAndEndAtIsNull(UUID employeeId);
    List<Employment> findByDepartmentId(UUID departmentId);
}
