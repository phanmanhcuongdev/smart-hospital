/**
 * Mô tả: Repository JPA truy vấn dữ liệu quyết định nhân sự
 * Người thực hiện: Tran Hoang Nam
 * Ngày thực hiện: 18/10/2026
 */

package vn.edu.ptit.smarthms.hr.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonnelDecisionRepository
        extends JpaRepository<PersonnelDecision, UUID> {
    List<PersonnelDecision> findByEmployeeId(UUID employeeId);
    boolean existsByDecisionRef(String decisionRef);
    Optional<PersonnelDecision> findByDecisionRef(String decisionRef);
    List<PersonnelDecision> findByDecisionStatusAndExecutionStatusAndEffectiveAtLessThanEqual(
            PersonnelDecision.DecisionStatus decisionStatus,
            PersonnelDecision.ExecutionStatus executionStatus, Instant effectiveAt);
}
