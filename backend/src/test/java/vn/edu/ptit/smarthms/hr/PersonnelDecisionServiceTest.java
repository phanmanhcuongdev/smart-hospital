package vn.edu.ptit.smarthms.hr;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import vn.edu.ptit.smarthms.common.error.ForbiddenException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.hr.api.HrRequests;
import vn.edu.ptit.smarthms.hr.domain.Employee;
import vn.edu.ptit.smarthms.hr.domain.EmployeeRepository;
import vn.edu.ptit.smarthms.hr.domain.EmployeeStatusHistory;
import vn.edu.ptit.smarthms.hr.domain.EmployeeStatusHistoryRepository;
import vn.edu.ptit.smarthms.hr.domain.IntegrationOutbox;
import vn.edu.ptit.smarthms.hr.domain.IntegrationOutboxRepository;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecision;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecisionRepository;
import vn.edu.ptit.smarthms.hr.service.PersonnelDecisionService;

@ExtendWith(MockitoExtension.class)
class PersonnelDecisionServiceTest {

    @Mock
    private PersonnelDecisionRepository decisions;

    @Mock
    private EmployeeRepository employees;

    @Mock
    private EmployeeStatusHistoryRepository histories;

    @Mock
    private IntegrationOutboxRepository outbox;

    @InjectMocks
    private PersonnelDecisionService service;

    private static final UUID EMPLOYEE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID DECISION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final Actor REQUESTER = new Actor("demo-hr", Set.of("HR_SPECIALIST"));

    private static final Actor REQUESTER_AS_APPROVER = new Actor("demo-hr", Set.of("HR_APPROVER"));

    private static final Actor APPROVER = new Actor("demo-hr-approver", Set.of("HR_APPROVER"));

    private static Employee activeEmployee() {
        Employee employee = new Employee("EMP-T-200", "Test Person");
        employee.setStatus(Employee.Status.ACTIVE);
        return employee;
    }

    private static PersonnelDecision decision(PersonnelDecision.DecisionType type, Instant effectiveAt) {
        PersonnelDecision decision = new PersonnelDecision(EMPLOYEE_ID, "DEC-1", type, effectiveAt);
        decision.setCreatedBy("demo-hr");
        return decision;
    }

    @Test
    void createDecisionKeepsFutureDecisionPending() {
        Instant effectiveAt = Instant.now().plusSeconds(86400);
        when(decisions.existsByDecisionRef("DEC-2")).thenReturn(false);
        when(employees.findById(EMPLOYEE_ID)).thenReturn(Optional.of(activeEmployee()));
        when(decisions.save(any(PersonnelDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.createDecision(REQUESTER, EMPLOYEE_ID,
                new HrRequests.CreateDecisionRequest("DEC-2", PersonnelDecision.DecisionType.SUSPEND, effectiveAt, "Planned suspension"));
        ArgumentCaptor<PersonnelDecision> captor = ArgumentCaptor.forClass(PersonnelDecision.class);
        verify(decisions).save(captor.capture());
        assertEquals(PersonnelDecision.DecisionStatus.DRAFT, captor.getValue().getDecisionStatus());
        assertEquals(PersonnelDecision.ExecutionStatus.PENDING, captor.getValue().getExecutionStatus());
        assertEquals(effectiveAt, captor.getValue().getEffectiveAt());
        verify(histories, never()).save(any());
        verify(outbox, never()).save(any());
    }

    @Test
    void approveDecisionRejectsSameRequester() {
        PersonnelDecision pending = decision(PersonnelDecision.DecisionType.SUSPEND, Instant.now().plusSeconds(3600));
        when(decisions.findById(DECISION_ID)).thenReturn(Optional.of(pending));
        assertThrows(ForbiddenException.class, () -> service.approveDecision(REQUESTER_AS_APPROVER, DECISION_ID));
    }

    @Test
    void approveDecisionByDifferentApproverMarksApproved() {
        PersonnelDecision pending = decision(PersonnelDecision.DecisionType.SUSPEND, Instant.now().plusSeconds(3600));
        when(decisions.findById(DECISION_ID)).thenReturn(Optional.of(pending));
        when(decisions.save(any(PersonnelDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.approveDecision(APPROVER, DECISION_ID);
        ArgumentCaptor<PersonnelDecision> captor = ArgumentCaptor.forClass(PersonnelDecision.class);
        verify(decisions).save(captor.capture());
        assertEquals(PersonnelDecision.DecisionStatus.APPROVED, captor.getValue().getDecisionStatus());
        assertEquals("demo-hr-approver", captor.getValue().getApprovedBy());
        assertNotNull(captor.getValue().getApprovedAt());
    }

    @Test
    void applyDueDecisionsAppliesTerminatedAndWritesOutbox() {
        PersonnelDecision terminate = decision(PersonnelDecision.DecisionType.TERMINATE, Instant.now().minusSeconds(3600));
        terminate.setDecisionStatus(PersonnelDecision.DecisionStatus.APPROVED);
        terminate.setApprovedBy("demo-hr-approver");
        terminate.setApprovedAt(Instant.now().minusSeconds(1800));
        when(decisions.findByDecisionStatusAndExecutionStatusAndEffectiveAtLessThanEqual(
                eq(PersonnelDecision.DecisionStatus.APPROVED), eq(PersonnelDecision.ExecutionStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(terminate));
        when(employees.findById(EMPLOYEE_ID)).thenReturn(Optional.of(activeEmployee()));
        when(employees.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(histories.save(any(EmployeeStatusHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outbox.save(any(IntegrationOutbox.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(decisions.save(any(PersonnelDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.applyDueDecisions(APPROVER);
        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employees).save(employeeCaptor.capture());
        assertEquals(Employee.Status.TERMINATED, employeeCaptor.getValue().getStatus());
        ArgumentCaptor<IntegrationOutbox> outboxCaptor = ArgumentCaptor.forClass(IntegrationOutbox.class);
        verify(outbox).save(outboxCaptor.capture());
        assertTrue(outboxCaptor.getValue().getEventKey().startsWith("EMPLOYEE_TERMINATED:"));
        ArgumentCaptor<PersonnelDecision> decisionCaptor = ArgumentCaptor.forClass(PersonnelDecision.class);
        verify(decisions).save(decisionCaptor.capture());
        assertEquals(PersonnelDecision.ExecutionStatus.APPLIED, decisionCaptor.getValue().getExecutionStatus());
        verify(histories, times(1)).save(any(EmployeeStatusHistory.class));
    }

    @Test
    void applyDueDecisionsOnlyQueriesDecisionsDueNow() {
        when(decisions.findByDecisionStatusAndExecutionStatusAndEffectiveAtLessThanEqual(
                eq(PersonnelDecision.DecisionStatus.APPROVED), eq(PersonnelDecision.ExecutionStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of());
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        service.applyDueDecisions(APPROVER);
        verify(decisions).findByDecisionStatusAndExecutionStatusAndEffectiveAtLessThanEqual(
                eq(PersonnelDecision.DecisionStatus.APPROVED), eq(PersonnelDecision.ExecutionStatus.PENDING), instantCaptor.capture());
        assertFalse(instantCaptor.getValue().isAfter(Instant.now()));
        verify(employees, never()).save(any());
        verify(outbox, never()).save(any());
    }

    @Test
    void applyDueDecisionsSkipsTransferDecisions() {
        PersonnelDecision transfer = decision(PersonnelDecision.DecisionType.TRANSFER, Instant.now().minusSeconds(3600));
        transfer.setDecisionStatus(PersonnelDecision.DecisionStatus.APPROVED);
        when(decisions.findByDecisionStatusAndExecutionStatusAndEffectiveAtLessThanEqual(
                eq(PersonnelDecision.DecisionStatus.APPROVED), eq(PersonnelDecision.ExecutionStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(transfer));
        service.applyDueDecisions(APPROVER);
        verify(decisions, never()).save(any(PersonnelDecision.class));
        verify(employees, never()).save(any());
        verify(histories, never()).save(any());
    }
}
