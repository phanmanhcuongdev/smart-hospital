package vn.edu.ptit.smarthms.hr.service;

import vn.edu.ptit.smarthms.hr.domain.PersonnelDecisionRepository;
import vn.edu.ptit.smarthms.hr.domain.LicenseCertificationRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.error.NotFoundException;
import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.hr.api.HrRequests.CreateEmployeeRequest;
import vn.edu.ptit.smarthms.hr.api.HrRequests.UpdateEmployeeRequest;
import vn.edu.ptit.smarthms.hr.api.HrResponses.AssignmentResponse;
import vn.edu.ptit.smarthms.hr.api.HrResponses.EmployeeResponse;
import vn.edu.ptit.smarthms.hr.api.HrResponses.EmployeeStatusHistoryResponse;
import vn.edu.ptit.smarthms.hr.domain.Department;
import vn.edu.ptit.smarthms.hr.domain.DepartmentRepository;
import vn.edu.ptit.smarthms.hr.domain.Employee;
import vn.edu.ptit.smarthms.hr.domain.EmployeeRepository;
import vn.edu.ptit.smarthms.hr.domain.EmployeeStatusHistory;
import vn.edu.ptit.smarthms.hr.domain.EmployeeStatusHistoryRepository;
import vn.edu.ptit.smarthms.hr.domain.Employment;
import vn.edu.ptit.smarthms.hr.domain.EmploymentRepository;
import vn.edu.ptit.smarthms.hr.domain.RoleProfileRepository;

/** Employees (FR-HR-01). Creating an employee grants no system role (BR-015). */
@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository employees;
    private final DepartmentRepository departments;
    private final RoleProfileRepository roleProfiles;
    private final EmploymentRepository employments;
  private final LicenseCertificationRepository licenses;
  private final PersonnelDecisionRepository decisions;
    private final EmployeeStatusHistoryRepository histories;

    public EmployeeService(EmployeeRepository employees, DepartmentRepository departments,
            RoleProfileRepository roleProfiles, EmploymentRepository employments,
            EmployeeStatusHistoryRepository histories, LicenseCertificationRepository licenses, PersonnelDecisionRepository decisions) {
    this.licenses = licenses;
    this.decisions = decisions;
        this.employees = employees;
        this.departments = departments;
        this.roleProfiles = roleProfiles;
        this.employments = employments;
        this.histories = histories;
    }

    public EmployeeResponse createEmployee(Actor actor, CreateEmployeeRequest request) {
        actor.requireRole("HR_SPECIALIST");
        if (employees.existsByEmployeeCode(request.employeeCode())) {
            throw new ConflictException("Employee code already exists: " + request.employeeCode());
        }
        Department department = departments.findById(request.departmentId())
                .orElseThrow(() -> NotFoundException.forId("Department", request.departmentId()));
        if (department.isActive() == false) {
            throw new ValidationException("Department is not active: " + department.getDepartmentCode());
        }
        if (request.roleProfileId() != null
                && roleProfiles.findById(request.roleProfileId()).isEmpty()) {
            throw NotFoundException.forId("Role profile", request.roleProfileId());
        }
        Instant startAt = request.hireDate() == null ? Instant.now()
                : request.hireDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Employee employee = new Employee(request.employeeCode(), request.fullName());
        employee.setBirthDate(request.birthDate());
        employee.setStatusEffectiveAt(startAt);
        employee.setGender(request.gender());
        employee.setAddress(request.address());
        employee.setPhone(request.phone());
        employee.setEmail(request.email());
        employee.setHireDate(request.hireDate());
        employee.setCreatedBy(actor.principalName());
        employee.setUpdatedBy(actor.principalName());
        Employee saved = employees.save(employee);
        Employment employment = new Employment(saved.getId(), request.departmentId(),
                request.jobTitle(), startAt);
        employment.setRoleProfileId(request.roleProfileId());
        employment.setCreatedBy(actor.principalName());
        employment.setUpdatedBy(actor.principalName());
        employments.save(employment);
        EmployeeStatusHistory history = new EmployeeStatusHistory(saved.getId(), null,
                Employee.Status.ONBOARDING, startAt, null);
        history.setCreatedBy(actor.principalName());
        history.setUpdatedBy(actor.principalName());
        histories.save(history);
        return EmployeeResponse.from(saved);
    }

    public EmployeeResponse updateEmployee(Actor actor, UUID employeeId,
            UpdateEmployeeRequest request) {
        actor.requireRole("HR_SPECIALIST");
        Employee employee = requireEmployee(employeeId);
        if (request.fullName() != null) {
            employee.setFullName(request.fullName());
        }
        if (request.birthDate() != null) {
            employee.setBirthDate(request.birthDate());
        }
        if (request.gender() != null) {
            employee.setGender(request.gender());
        }
        if (request.address() != null) {
            employee.setAddress(request.address());
        }
        if (request.phone() != null) {
            employee.setPhone(request.phone());
        }
        if (request.email() != null) {
            employee.setEmail(request.email());
        }
        employee.setUpdatedBy(actor.principalName());
        return EmployeeResponse.from(employees.save(employee));
    }

    public void deleteEmployee(Actor actor, UUID employeeId) {
    actor.requireRole("HR_SPECIALIST");
    Employee employee = requireEmployee(employeeId);
    if (employee.getStatus() == Employee.Status.TERMINATED) {
      throw new ConflictException("Cannot delete a terminated employee: the personnel record must be retained.");
    }
    if (!licenses.findByEmployeeId(employeeId).isEmpty()) {
      throw new ConflictException("Cannot delete employee: they still have certifications.");
    }
    if (!decisions.findByEmployeeId(employeeId).isEmpty()) {
      throw new ConflictException("Cannot delete employee: they still have personnel decisions.");
    }
    employments.deleteAll(employments.findByEmployeeId(employeeId));
    histories.deleteAll(histories.findByEmployeeIdOrderByEffectiveAtAsc(employeeId));
    employees.delete(employee);
  }

  public EmployeeResponse getEmployee(Actor actor, UUID employeeId) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        return EmployeeResponse.from(requireEmployee(employeeId));
    }

    private static String fold(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.toLowerCase(java.util.Locale.ROOT).replace('\u0111', 'd');
    }

    public List<EmployeeResponse> searchEmployees(Actor actor, UUID departmentId,
            String status, String jobTitle, String query) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        Employee.Status wanted = null;
        if (status != null) {
            try {
                wanted = Employee.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Unknown employee status: " + status);
            }
        }
        Set<UUID> inDepartment = null;
        if (departmentId != null) {
            inDepartment = new HashSet<>();
            for (Employment employment : employments.findByDepartmentId(departmentId)) {
                inDepartment.add(employment.getEmployeeId());
            }
        }
        List<EmployeeResponse> result = new ArrayList<>();
        for (Employee employee : employees.findAll()) {
            if (wanted != null && employee.getStatus() != wanted) {
                continue;
            }
            if (inDepartment != null && inDepartment.contains(employee.getId()) == false) {
                continue;
            }
            if (jobTitle != null && matchesJobTitle(employee.getId(), jobTitle) == false) {
                continue;
            }
            if (query != null && query.isBlank() == false) {
                String needle = fold(query.trim());
                String name = fold(employee.getFullName());
                String code = fold(employee.getEmployeeCode());
                if (name.contains(needle) == false && code.contains(needle) == false) {
                    continue;
                }
            }
            result.add(EmployeeResponse.from(employee));
        }
        return result;
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

    public List<EmployeeStatusHistoryResponse> listStatusHistory(Actor actor, UUID employeeId) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        requireEmployee(employeeId);
        List<EmployeeStatusHistoryResponse> result = new ArrayList<>();
        for (EmployeeStatusHistory history : histories.findByEmployeeIdOrderByEffectiveAtAsc(employeeId)) {
            result.add(EmployeeStatusHistoryResponse.from(history));
        }
        return result;
    }

    public Employee requireEmployee(UUID employeeId) {
        return employees.findById(employeeId)
                .orElseThrow(() -> NotFoundException.forId("Employee", employeeId));
    }

    private boolean matchesJobTitle(UUID employeeId, String jobTitle) {
        for (Employment employment : employments.findByEmployeeId(employeeId)) {
            if (employment.getJobTitle().equalsIgnoreCase(jobTitle)) {
                return true;
            }
        }
        return false;
    }
}
