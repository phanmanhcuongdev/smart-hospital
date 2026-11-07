package vn.edu.ptit.smarthms.hr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeStatusHistoryRepository
        extends JpaRepository<EmployeeStatusHistory, UUID> {
    List<EmployeeStatusHistory> findByEmployeeIdOrderByEffectiveAtAsc(UUID employeeId);
}
