package vn.edu.ptit.smarthms.hr.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.error.ForbiddenException;
import vn.edu.ptit.smarthms.common.error.NotFoundException;
import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.hr.api.HrRequests.CreateDecisionRequest;
import vn.edu.ptit.smarthms.hr.api.HrResponses.DecisionResponse;
import vn.edu.ptit.smarthms.hr.domain.Employee;
import vn.edu.ptit.smarthms.hr.domain.EmployeeRepository;
import vn.edu.ptit.smarthms.hr.domain.EmployeeStatusHistory;
import vn.edu.ptit.smarthms.hr.domain.EmployeeStatusHistoryRepository;
import vn.edu.ptit.smarthms.hr.domain.IntegrationOutbox;
import vn.edu.ptit.smarthms.hr.domain.IntegrationOutboxRepository;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecision;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecisionRepository;

/** Status and transfer decisions (FR-HR-03, FR-HR-04, BR-017, BR-019, BR-020, BR-090). */
@Service
@Transactional
public class PersonnelDecisionService {

    private final PersonnelDecisionRepository decisions;
    private final EmployeeRepository employees;
    private final EmployeeStatusHistoryRepository histories;
    private final IntegrationOutboxRepository outbox;

    public PersonnelDecisionService(PersonnelDecisionRepository decisions,
            EmployeeRepository employees, EmployeeStatusHistoryRepository histories,
            IntegrationOutboxRepository outbox) {
        this.decisions = decisions;
        this.employees = employees;
        this.histories = histories;
        this.outbox = outbox;
    }

    public DecisionResponse createDecision(Actor actor, UUID employeeId,
            CreateDecisionRequest request) {
        actor.requireRole("HR_SPECIALIST");
        requireEmployee(employeeId);
        if (decisions.existsByDecisionRef(request.decisionRef())) {
            throw new ConflictException("Decision reference already exists: " + request.decisionRef());
        }
        PersonnelDecision decision = new PersonnelDecision(employeeId, request.decisionRef(),
                request.decisionType(), request.effectiveAt());
        decision.setReason(request.reason());
        decision.setCreatedBy(actor.principalName());
        decision.setUpdatedBy(actor.principalName());
        return DecisionResponse.from(decisions.save(decision));
    }

    public DecisionResponse approveDecision(Actor actor, UUID decisionId) {
        actor.requireRole("HR_APPROVER");
        PersonnelDecision decision = requireDecision(decisionId);
        if (decision.getDecisionStatus() != PersonnelDecision.DecisionStatus.DRAFT) {
            throw new ConflictException("Only a DRAFT decision can be approved.");
        }
        if (actor.principalName().equals(decision.getCreatedBy())) {
            throw new ForbiddenException("Approver must differ from the requester (four-eyes).");
        }
        decision.setDecisionStatus(PersonnelDecision.DecisionStatus.APPROVED);
        decision.setApprovedBy(actor.principalName());
        decision.setApprovedAt(Instant.now());
        decision.setUpdatedBy(actor.principalName());
        PersonnelDecision saved = decisions.save(decision);
        if (saved.getEffectiveAt().isAfter(Instant.now()) == false) {
            applyDecision(saved, actor.principalName());
        }
        return DecisionResponse.from(saved);
    }

    public DecisionResponse getDecision(Actor actor, UUID decisionId) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        return DecisionResponse.from(requireDecision(decisionId));
    }

    public List<DecisionResponse> listDecisions(Actor actor, UUID employeeId, String status) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        PersonnelDecision.DecisionStatus wanted = null;
        if (status != null) {
            try {
                wanted = PersonnelDecision.DecisionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Unknown decision status: " + status);
            }
        }
        List<DecisionResponse> result = new ArrayList<>();
        List<PersonnelDecision> found = employeeId == null
                ? decisions.findAll() : decisions.findByEmployeeId(employeeId);
        for (PersonnelDecision decision : found) {
            if (wanted != null && decision.getDecisionStatus() != wanted) {
                continue;
            }
            result.add(DecisionResponse.from(decision));
        }
        return result;
    }

    public List<DecisionResponse> applyDueDecisions(Actor actor) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        List<DecisionResponse> result = new ArrayList<>();
        for (PersonnelDecision decision : decisions.findByDecisionStatusAndExecutionStatusAndEffectiveAtLessThanEqual(
                PersonnelDecision.DecisionStatus.APPROVED,
                PersonnelDecision.ExecutionStatus.PENDING, Instant.now())) {
            if (decision.getDecisionType() == PersonnelDecision.DecisionType.TRANSFER) {
                continue;
            }
            applyDecision(decision, actor.principalName());
            result.add(DecisionResponse.from(decisions.save(decision)));
        }
        return result;
    }

    public DecisionResponse revokeDecision(Actor actor, UUID decisionId) {
        actor.requireRole("HR_APPROVER");
        PersonnelDecision decision = requireDecision(decisionId);
        if (decision.getDecisionStatus() == PersonnelDecision.DecisionStatus.REVOKED) {
            throw new ConflictException("Decision is already revoked.");
        }
        if (decision.getExecutionStatus() == PersonnelDecision.ExecutionStatus.APPLIED) {
            throw new ConflictException("Decision already applied and cannot be revoked.");
        }
        decision.setDecisionStatus(PersonnelDecision.DecisionStatus.REVOKED);
        decision.setExecutionStatus(PersonnelDecision.ExecutionStatus.SKIPPED);
        decision.setUpdatedBy(actor.principalName());
        return DecisionResponse.from(decisions.save(decision));
    }

    private void applyDecision(PersonnelDecision decision, String appliedBy) {
        if (decision.getDecisionType() == PersonnelDecision.DecisionType.TRANSFER) {
            return;
        }
        Employee.Status target = switch (decision.getDecisionType()) {
            case ACTIVATE -> Employee.Status.ACTIVE;
            case RETURN -> Employee.Status.ACTIVE;
            case SUSPEND -> Employee.Status.SUSPENDED;
            case TERMINATE -> Employee.Status.TERMINATED;
            case TRANSFER -> Employee.Status.ACTIVE;
        };
        Employee employee = requireEmployee(decision.getEmployeeId());
        Employee.Status oldStatus = employee.getStatus();
        employee.setStatus(target);
        employee.setStatusEffectiveAt(decision.getEffectiveAt());
        employee.setUpdatedBy(appliedBy);
        employees.save(employee);
        EmployeeStatusHistory history = new EmployeeStatusHistory(employee.getId(), oldStatus,
                target, decision.getEffectiveAt(), decision.getId());
        history.setCreatedBy(appliedBy);
        history.setUpdatedBy(appliedBy);
        histories.save(history);
        if (decision.getDecisionType() == PersonnelDecision.DecisionType.TERMINATE) {
            writeTerminationOutbox(decision, appliedBy);
        }
        decision.setExecutionStatus(PersonnelDecision.ExecutionStatus.APPLIED);
    }

    private void writeTerminationOutbox(PersonnelDecision decision, String writtenBy) {
        String eventKey = "EMPLOYEE_TERMINATED:" + decision.getEmployeeId() + ":" + decision.getId();
        if (outbox.existsByEventKey(eventKey)) {
            return;
        }
        String payload = "{\"eventType\":\"EMPLOYEE_TERMINATED\",\"employeeId\":\""
                + decision.getEmployeeId() + "\",\"decisionId\":\"" + decision.getId()
                + "\",\"effectiveAt\":\"" + decision.getEffectiveAt() + "\"}";
        IntegrationOutbox event = new IntegrationOutbox(eventKey, decision.getEmployeeId(), payload);
        event.setCreatedBy(writtenBy);
        event.setUpdatedBy(writtenBy);
        outbox.save(event);
    }

    private PersonnelDecision requireDecision(UUID decisionId) {
        return decisions.findById(decisionId)
                .orElseThrow(() -> new NotFoundException("Decision not found: " + decisionId));
    }

    public Employee requireEmployee(UUID employeeId) {
        return employees.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found: " + employeeId));
    }
}
