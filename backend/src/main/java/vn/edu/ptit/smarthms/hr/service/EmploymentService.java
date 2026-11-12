package vn.edu.ptit.smarthms.hr.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.error.NotFoundException;
import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.hr.api.HrRequests.CreateTransferRequest;
import vn.edu.ptit.smarthms.hr.api.HrResponses.AssignmentResponse;
import vn.edu.ptit.smarthms.hr.domain.Department;
import vn.edu.ptit.smarthms.hr.domain.DepartmentRepository;
import vn.edu.ptit.smarthms.hr.domain.Employee;
import vn.edu.ptit.smarthms.hr.domain.EmployeeRepository;
import vn.edu.ptit.smarthms.hr.domain.Employment;
import vn.edu.ptit.smarthms.hr.domain.EmploymentRepository;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecision;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecisionRepository;
import vn.edu.ptit.smarthms.hr.domain.RoleProfileRepository;

/** Transfers (FR-HR-04, BR-019, BR-020). History is never overwritten. */
@Service
@Transactional
public class EmploymentService {

    private final EmploymentRepository employments;
    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final RoleProfileRepository roleProfiles;
    private final PersonnelDecisionRepository decisions;

    public EmploymentService(EmploymentRepository employments, EmployeeRepository employees,
            DepartmentRepository departments, RoleProfileRepository roleProfiles,
            PersonnelDecisionRepository decisions) {
        this.employments = employments;
        this.employees = employees;
        this.departments = departments;
        this.roleProfiles = roleProfiles;
        this.decisions = decisions;
    }

    public AssignmentResponse createTransfer(Actor actor, UUID employeeId,
            CreateTransferRequest request) {
        actor.requireRole("HR_SPECIALIST");
        Employee employee = requireEmployee(employeeId);
        Department department = departments.findById(request.departmentId())
                .orElseThrow(() -> NotFoundException.forId("Department", request.departmentId()));
        if (department.isActive() == false) {
            throw new ValidationException("Department is not active: " + department.getDepartmentCode());
        }
        if (request.roleProfileId() != null
                && roleProfiles.findById(request.roleProfileId()).isEmpty()) {
            throw NotFoundException.forId("Role profile", request.roleProfileId());
        }
        if (request.decisionRef() != null) {
            PersonnelDecision decision = decisions.findByDecisionRef(request.decisionRef())
                    .orElseThrow(() -> new ValidationException("Transfer decision not found: "
                            + request.decisionRef()));
            if (decision.getDecisionType() != PersonnelDecision.DecisionType.TRANSFER
                    || decision.getDecisionStatus() != PersonnelDecision.DecisionStatus.APPROVED) {
                throw new ValidationException("Transfer needs an APPROVED TRANSFER decision.");
            }
            decision.setExecutionStatus(PersonnelDecision.ExecutionStatus.APPLIED);
            decisions.save(decision);
        }
        if (request.effectiveFrom().isBefore(Instant.now().minusSeconds(60))) {
            throw new ValidationException("effectiveFrom must not be in the past.");
        }
        List<Employment> toClose = new ArrayList<>();
        for (Employment existing : employments.findByEmployeeId(employee.getId())) {
            if (existing.isOpen()) {
                if (existing.getStartAt().compareTo(request.effectiveFrom()) >= 0) {
                    throw new ConflictException("Cannot transfer before the current assignment starts.");
                }
                toClose.add(existing);
            } else if (existing.getEndAt().isAfter(request.effectiveFrom())) {
                throw new ConflictException("Overlapping assignment exists.");
            }
        }
        for (Employment existing : toClose) {
            existing.setEndAt(request.effectiveFrom());
            existing.setUpdatedBy(actor.principalName());
            employments.save(existing);
        }
        Employment transfer = new Employment(employee.getId(), request.departmentId(),
                request.jobTitle(), request.effectiveFrom());
        transfer.setRoleProfileId(request.roleProfileId());
        transfer.setDecisionRef(request.decisionRef());
        transfer.setCreatedBy(actor.principalName());
        transfer.setUpdatedBy(actor.principalName());
        Employment saved = employments.save(transfer);
        return AssignmentResponse.from(saved, department.getDepartmentCode());
    }

    public List<AssignmentResponse> listAssignments(Actor actor, UUID employeeId) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        requireEmployee(employeeId);
        List<AssignmentResponse> result = new ArrayList<>();
        for (Employment employment : employments.findByEmployeeId(employeeId)) {
            String departmentCode = departments.findById(employment.getDepartmentId())
                    .map(Department::getDepartmentCode).orElse(null);
            result.add(AssignmentResponse.from(employment, departmentCode));
        }
        return result;
    }

    private Employee requireEmployee(UUID employeeId) {
        return employees.findById(employeeId)
                .orElseThrow(() -> NotFoundException.forId("Employee", employeeId));
    }
}
