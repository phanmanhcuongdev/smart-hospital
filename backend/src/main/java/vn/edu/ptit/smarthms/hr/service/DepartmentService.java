package vn.edu.ptit.smarthms.hr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.error.NotFoundException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.hr.api.HrRequests.CreateDepartmentRequest;
import vn.edu.ptit.smarthms.hr.api.HrRequests.UpdateDepartmentRequest;
import vn.edu.ptit.smarthms.hr.api.HrResponses.DepartmentResponse;
import vn.edu.ptit.smarthms.hr.domain.Department;
import vn.edu.ptit.smarthms.hr.domain.DepartmentRepository;

/** Departments (FR-HR-01, BR-015). */
@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departments;

    public DepartmentService(DepartmentRepository departments) {
        this.departments = departments;
    }

    public DepartmentResponse createDepartment(Actor actor, CreateDepartmentRequest request) {
        actor.requireRole("HR_SPECIALIST");
        if (departments.existsByDepartmentCode(request.departmentCode())) {
            throw new ConflictException("Department code already exists: " + request.departmentCode());
        }
        if (request.parentDepartmentId() != null) {
            requireDepartment(request.parentDepartmentId());
        }
        Department department = new Department(request.departmentCode(), request.displayName());
        department.setParentDepartmentId(request.parentDepartmentId());
        department.setDepartmentType(request.departmentType());
        if (request.active() != null) {
            department.setActive(request.active());
        }
        department.setCreatedBy(actor.principalName());
        department.setUpdatedBy(actor.principalName());
        return DepartmentResponse.from(departments.save(department));
    }

    public DepartmentResponse updateDepartment(Actor actor, UUID departmentId,
            UpdateDepartmentRequest request) {
        actor.requireRole("HR_SPECIALIST");
        Department department = requireDepartment(departmentId);
        if (request.displayName() != null) {
            department.setDisplayName(request.displayName());
        }
        if (request.parentDepartmentId() != null) {
            Department parent = requireDepartment(request.parentDepartmentId());
            if (parent.getId().equals(departmentId)) {
                throw new ConflictException("A department cannot be its own parent.");
            }
            department.setParentDepartmentId(request.parentDepartmentId());
        }
        if (request.departmentType() != null) {
            department.setDepartmentType(request.departmentType());
        }
        if (request.active() != null) {
            department.setActive(request.active());
        }
        department.setUpdatedBy(actor.principalName());
        return DepartmentResponse.from(departments.save(department));
    }

    public DepartmentResponse getDepartment(Actor actor, UUID departmentId) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        return DepartmentResponse.from(requireDepartment(departmentId));
    }

    public List<DepartmentResponse> listDepartments(Actor actor, Boolean active) {
        actor.requireAnyRole("HR_SPECIALIST", "HR_APPROVER");
        List<Department> found = active == null
                ? departments.findAll() : departments.findByActive(active);
        List<DepartmentResponse> result = new ArrayList<>();
        for (Department department : found) {
            result.add(DepartmentResponse.from(department));
        }
        return result;
    }

    public Department requireDepartment(UUID departmentId) {
        return departments.findById(departmentId)
                .orElseThrow(() -> new NotFoundException("Department not found: " + departmentId));
    }
}
