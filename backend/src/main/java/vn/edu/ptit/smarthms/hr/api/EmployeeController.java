package vn.edu.ptit.smarthms.hr.api;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.hr.api.HrRequests.CreateEmployeeRequest;
import vn.edu.ptit.smarthms.hr.api.HrRequests.UpdateEmployeeRequest;
import vn.edu.ptit.smarthms.hr.api.HrResponses.AssignmentResponse;
import vn.edu.ptit.smarthms.hr.api.HrResponses.EmployeeResponse;
import vn.edu.ptit.smarthms.hr.api.HrResponses.EmployeeStatusHistoryResponse;
import vn.edu.ptit.smarthms.hr.service.EmployeeService;

/** Employees (FR-HR-01, FR-HR-03). */
@RestController
@RequestMapping("/api/v1/hr/employees")
public class EmployeeController {

    private final EmployeeService employees;

    public EmployeeController(EmployeeService employees) {
        this.employees = employees;
    }

    private Actor actor(Authentication authentication) {
        return ActorResolver.from(authentication);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<EmployeeResponse> createEmployee(Authentication authentication,
            @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employees.createEmployee(actor(authentication), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER', 'ADMIN')")
    public List<EmployeeResponse> searchEmployees(Authentication authentication,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String q) {
        return employees.searchEmployees(actor(authentication), departmentId, status, jobTitle, q);
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER', 'ADMIN')")
    public EmployeeResponse getEmployee(Authentication authentication,
            @PathVariable UUID employeeId) {
        return employees.getEmployee(actor(authentication), employeeId);
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public EmployeeResponse updateEmployee(Authentication authentication,
            @PathVariable UUID employeeId, @Valid @RequestBody UpdateEmployeeRequest request) {
        return employees.updateEmployee(actor(authentication), employeeId, request);
    }

    @GetMapping("/{employeeId}/assignments")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER', 'ADMIN')")
    public List<AssignmentResponse> listAssignments(Authentication authentication,
            @PathVariable UUID employeeId) {
        return employees.listAssignments(actor(authentication), employeeId);
    }

    @GetMapping("/{employeeId}/status-history")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER', 'ADMIN')")
    public List<EmployeeStatusHistoryResponse> listStatusHistory(Authentication authentication,
            @PathVariable UUID employeeId) {
        return employees.listStatusHistory(actor(authentication), employeeId);
    }

  @DeleteMapping("/{employeeId}")
  @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
  public ResponseEntity<Void> deleteEmployee(Authentication authentication, @PathVariable UUID employeeId) {
    employees.deleteEmployee(actor(authentication), employeeId);
    return ResponseEntity.noContent().build();
  }
}
