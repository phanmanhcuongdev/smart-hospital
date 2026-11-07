package vn.edu.ptit.smarthms.hr;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.hr.api.HrRequests;
import vn.edu.ptit.smarthms.hr.domain.Department;
import vn.edu.ptit.smarthms.hr.domain.DepartmentRepository;
import vn.edu.ptit.smarthms.hr.domain.Employee;
import vn.edu.ptit.smarthms.hr.domain.EmployeeRepository;
import vn.edu.ptit.smarthms.hr.domain.Employment;
import vn.edu.ptit.smarthms.hr.domain.EmploymentRepository;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecision;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecisionRepository;
import vn.edu.ptit.smarthms.hr.domain.RoleProfileRepository;
import vn.edu.ptit.smarthms.hr.service.EmploymentService;

@ExtendWith(MockitoExtension.class)
class EmploymentServiceTest {

    @Mock
    private EmploymentRepository employments;

    @Mock
    private EmployeeRepository employees;

    @Mock
    private DepartmentRepository departments;

    @Mock
    private RoleProfileRepository roleProfiles;

    @Mock
    private PersonnelDecisionRepository decisions;

    @InjectMocks
    private EmploymentService service;

    private static final UUID EMPLOYEE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID OLD_DEPARTMENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final UUID NEW_DEPARTMENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final Actor HR = new Actor("demo-hr", Set.of("HR_SPECIALIST"));

    private void stubEmployeeAndDepartment() {
        Employee employee = mock(Employee.class);
        lenient().when(employee.getId()).thenReturn(EMPLOYEE_ID);
        when(employees.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        Department department = mock(Department.class);
        when(department.isActive()).thenReturn(true);
        when(departments.findById(NEW_DEPARTMENT_ID)).thenReturn(Optional.of(department));
    }

    private HrRequests.CreateTransferRequest transferRequest(Instant effectiveFrom, String decisionRef) {
        return new HrRequests.CreateTransferRequest(NEW_DEPARTMENT_ID, "Senior Nurse", null, effectiveFrom, decisionRef);
    }

    @Test
    void transferClosesOldAndOpensNew() {
        stubEmployeeAndDepartment();
        Instant effectiveFrom = Instant.now().plusSeconds(3600);
        Employment old = new Employment(EMPLOYEE_ID, OLD_DEPARTMENT_ID, "Nurse", Instant.now().minusSeconds(3600));
        when(employments.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(old));
        when(employments.save(any(Employment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.createTransfer(HR, EMPLOYEE_ID, transferRequest(effectiveFrom, null));
        ArgumentCaptor<Employment> captor = ArgumentCaptor.forClass(Employment.class);
        verify(employments, times(2)).save(captor.capture());
        List<Employment> saved = captor.getAllValues();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().anyMatch(e -> e.getEndAt() != null && e.getEndAt().equals(effectiveFrom) && e.getDepartmentId().equals(OLD_DEPARTMENT_ID)));
        assertTrue(saved.stream().anyMatch(e -> e.getStartAt().equals(effectiveFrom) && e.getDepartmentId().equals(NEW_DEPARTMENT_ID) && e.getEndAt() == null));
    }

    @Test
    void transferRejectsSameInstantStart() {
        stubEmployeeAndDepartment();
        Instant effectiveFrom = Instant.now().plusSeconds(3600);
        Employment open = new Employment(EMPLOYEE_ID, OLD_DEPARTMENT_ID, "Nurse", effectiveFrom);
        when(employments.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(open));
        assertThrows(ConflictException.class, () -> service.createTransfer(HR, EMPLOYEE_ID, transferRequest(effectiveFrom, null)));
        verify(employments, never()).save(any(Employment.class));
    }

    @Test
    void transferRejectsOverlapWithClosedAssignment() {
        stubEmployeeAndDepartment();
        Instant effectiveFrom = Instant.now().plusSeconds(3600);
        Employment closed = new Employment(EMPLOYEE_ID, OLD_DEPARTMENT_ID, "Nurse", Instant.now().minusSeconds(86400));
        closed.setEndAt(effectiveFrom.plusSeconds(3600));
        when(employments.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(closed));
        assertThrows(ConflictException.class, () -> service.createTransfer(HR, EMPLOYEE_ID, transferRequest(effectiveFrom, null)));
        verify(employments, never()).save(any(Employment.class));
    }

    @Test
    void transferWithApprovedDecisionMarksDecisionApplied() {
        stubEmployeeAndDepartment();
        Instant effectiveFrom = Instant.now().plusSeconds(3600);
        PersonnelDecision transfer = new PersonnelDecision(EMPLOYEE_ID, "DEC-TR-1", PersonnelDecision.DecisionType.TRANSFER, effectiveFrom);
        transfer.setDecisionStatus(PersonnelDecision.DecisionStatus.APPROVED);
        when(decisions.findByDecisionRef("DEC-TR-1")).thenReturn(Optional.of(transfer));
        when(employments.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of());
        when(employments.save(any(Employment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(decisions.save(any(PersonnelDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.createTransfer(HR, EMPLOYEE_ID, transferRequest(effectiveFrom, "DEC-TR-1"));
        ArgumentCaptor<PersonnelDecision> captor = ArgumentCaptor.forClass(PersonnelDecision.class);
        verify(decisions).save(captor.capture());
        assertEquals(PersonnelDecision.ExecutionStatus.APPLIED, captor.getValue().getExecutionStatus());
    }

    @Test
    void transferWithDraftDecisionIsRejected() {
        stubEmployeeAndDepartment();
        Instant effectiveFrom = Instant.now().plusSeconds(3600);
        PersonnelDecision draft = new PersonnelDecision(EMPLOYEE_ID, "DEC-TR-2", PersonnelDecision.DecisionType.TRANSFER, effectiveFrom);
        when(decisions.findByDecisionRef("DEC-TR-2")).thenReturn(Optional.of(draft));
        assertThrows(ValidationException.class, () -> service.createTransfer(HR, EMPLOYEE_ID, transferRequest(effectiveFrom, "DEC-TR-2")));
        verify(employments, never()).save(any(Employment.class));
    }
}
