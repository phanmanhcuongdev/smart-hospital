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
import vn.edu.ptit.smarthms.hr.api.HrRequests.CreateDepartmentRequest;
import vn.edu.ptit.smarthms.hr.api.HrRequests.UpdateDepartmentRequest;
import vn.edu.ptit.smarthms.hr.api.HrResponses.DepartmentResponse;
import vn.edu.ptit.smarthms.hr.service.DepartmentService;

/** Departments (FR-HR-01). */
@RestController
@RequestMapping("/api/v1/hr/departments")
public class DepartmentController {

    private final DepartmentService departments;

    public DepartmentController(DepartmentService departments) {
        this.departments = departments;
    }

    private Actor actor(Authentication authentication) {
        return ActorResolver.from(authentication);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<DepartmentResponse> createDepartment(Authentication authentication,
            @Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departments.createDepartment(actor(authentication), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER', 'ADMIN')")
    public List<DepartmentResponse> listDepartments(Authentication authentication,
            @RequestParam(required = false) Boolean active) {
        return departments.listDepartments(actor(authentication), active);
    }

    @GetMapping("/{departmentId}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'HR_APPROVER', 'ADMIN')")
    public DepartmentResponse getDepartment(Authentication authentication,
            @PathVariable UUID departmentId) {
        return departments.getDepartment(actor(authentication), departmentId);
    }

    @PutMapping("/{departmentId}")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public DepartmentResponse updateDepartment(Authentication authentication,
            @PathVariable UUID departmentId, @Valid @RequestBody UpdateDepartmentRequest request) {
        return departments.updateDepartment(actor(authentication), departmentId, request);
    }

  @DeleteMapping("/{departmentId}")
  @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
  public ResponseEntity<Void> deleteDepartment(Authentication authentication, @PathVariable UUID departmentId) {
    departments.deleteDepartment(actor(authentication), departmentId);
    return ResponseEntity.noContent().build();
  }
}
