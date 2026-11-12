package vn.edu.ptit.smarthms.hr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.error.NotFoundException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.hr.domain.Department;
import vn.edu.ptit.smarthms.hr.domain.DepartmentRepository;
import vn.edu.ptit.smarthms.hr.domain.Employee;
import vn.edu.ptit.smarthms.hr.domain.Employment;
import vn.edu.ptit.smarthms.hr.domain.EmploymentRepository;
import vn.edu.ptit.smarthms.hr.service.DepartmentService;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

  private static final UUID DEPT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Mock
  DepartmentRepository departments;

  @Mock
  EmploymentRepository employments;

  @Mock
  ActorResolver actor;

  @InjectMocks
  DepartmentService service;

  private final Actor hr = new Actor("demo-hr", Set.of("HR_SPECIALIST"));

  @Test
  void deleteSucceedsWhenNoEmployeesOrChildren() {
    Department dept = new Department("KHOA-1", "Khoa mot");
    when(departments.findById(DEPT_ID)).thenReturn(Optional.of(dept));
    when(employments.findByDepartmentId(DEPT_ID)).thenReturn(List.of());
    when(departments.findAll()).thenReturn(List.of());
    service.deleteDepartment(hr, DEPT_ID);
    verify(departments).delete(dept);
  }

  @Test
  void deleteBlockedWhenEmployeesAssigned() {
    Department dept = new Department("KHOA-1", "Khoa mot");
    when(departments.findById(DEPT_ID)).thenReturn(Optional.of(dept));
    when(employments.findByDepartmentId(DEPT_ID)).thenReturn(List.of(mock(Employment.class)));
    assertThrows(ConflictException.class, () -> service.deleteDepartment(hr, DEPT_ID));
  }

  @Test
  void deleteBlockedWhenChildDepartmentExists() {
    Department dept = new Department("KHOA-1", "Khoa mot");
    Department child = new Department("KHOA-2", "Khoa hai");
    child.setParentDepartmentId(DEPT_ID);
    when(departments.findById(DEPT_ID)).thenReturn(Optional.of(dept));
    when(employments.findByDepartmentId(DEPT_ID)).thenReturn(List.of());
    when(departments.findAll()).thenReturn(List.of(child));
    assertThrows(ConflictException.class, () -> service.deleteDepartment(hr, DEPT_ID));
  }

  @Test
  void deleteMissingDepartmentThrows404() {
    when(departments.findById(DEPT_ID)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.deleteDepartment(hr, DEPT_ID));
  }
}
