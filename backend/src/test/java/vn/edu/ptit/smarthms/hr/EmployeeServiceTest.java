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
import vn.edu.ptit.smarthms.hr.domain.DepartmentRepository;
import vn.edu.ptit.smarthms.hr.domain.Employee;
import vn.edu.ptit.smarthms.hr.domain.EmployeeRepository;
import vn.edu.ptit.smarthms.hr.domain.EmployeeStatusHistory;
import vn.edu.ptit.smarthms.hr.domain.EmployeeStatusHistoryRepository;
import vn.edu.ptit.smarthms.hr.domain.Employment;
import vn.edu.ptit.smarthms.hr.domain.EmploymentRepository;
import vn.edu.ptit.smarthms.hr.domain.LicenseCertification;
import vn.edu.ptit.smarthms.hr.domain.LicenseCertificationRepository;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecision;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecisionRepository;
import vn.edu.ptit.smarthms.hr.domain.RoleProfileRepository;
import vn.edu.ptit.smarthms.hr.service.EmployeeService;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

  private static final UUID EMP_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  @Mock
  EmployeeRepository employees;

  @Mock
  DepartmentRepository departments;

  @Mock
  RoleProfileRepository roleProfiles;

  @Mock
  EmploymentRepository employments;

  @Mock
  EmployeeStatusHistoryRepository histories;

  @Mock
  LicenseCertificationRepository licenses;

  @Mock
  PersonnelDecisionRepository decisions;

  @Mock
  ActorResolver actor;

  @InjectMocks
  EmployeeService service;

  private final Actor hr = new Actor("demo-hr", Set.of("HR_SPECIALIST"));

  private Employee employee(Employee.Status status) {
    Employee e = new Employee("EMP-T-1", "Test One");
    e.setStatus(status);
    return e;
  }

  @Test
  void deleteSucceedsAndRemovesAssignmentsAndHistory() {
    Employee e = employee(Employee.Status.ACTIVE);
    Employment em = mock(Employment.class);
    EmployeeStatusHistory h = mock(EmployeeStatusHistory.class);
    when(employees.findById(EMP_ID)).thenReturn(Optional.of(e));
    when(licenses.findByEmployeeId(EMP_ID)).thenReturn(List.of());
    when(decisions.findByEmployeeId(EMP_ID)).thenReturn(List.of());
    when(employments.findByEmployeeId(EMP_ID)).thenReturn(List.of(em));
    when(histories.findByEmployeeIdOrderByEffectiveAtAsc(EMP_ID)).thenReturn(List.of(h));
    service.deleteEmployee(hr, EMP_ID);
    verify(employments).deleteAll(List.of(em));
    verify(histories).deleteAll(List.of(h));
    verify(employees).delete(e);
  }

  @Test
  void deleteTerminatedEmployeeIsBlocked() {
    Employee e = employee(Employee.Status.TERMINATED);
    when(employees.findById(EMP_ID)).thenReturn(Optional.of(e));
    assertThrows(ConflictException.class, () -> service.deleteEmployee(hr, EMP_ID));
  }

  @Test
  void deleteEmployeeWithCertificationsIsBlocked() {
    Employee e = employee(Employee.Status.ACTIVE);
    when(employees.findById(EMP_ID)).thenReturn(Optional.of(e));
    when(licenses.findByEmployeeId(EMP_ID)).thenReturn(List.of(mock(LicenseCertification.class)));
    assertThrows(ConflictException.class, () -> service.deleteEmployee(hr, EMP_ID));
  }

  @Test
  void deleteEmployeeWithDecisionsIsBlocked() {
    Employee e = employee(Employee.Status.ACTIVE);
    when(employees.findById(EMP_ID)).thenReturn(Optional.of(e));
    when(licenses.findByEmployeeId(EMP_ID)).thenReturn(List.of());
    when(decisions.findByEmployeeId(EMP_ID)).thenReturn(List.of(mock(PersonnelDecision.class)));
    assertThrows(ConflictException.class, () -> service.deleteEmployee(hr, EMP_ID));
  }

  @Test
  void deleteMissingEmployeeThrows404() {
    when(employees.findById(EMP_ID)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.deleteEmployee(hr, EMP_ID));
  }
}
