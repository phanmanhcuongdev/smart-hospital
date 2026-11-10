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
