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
import vn.edu.ptit.smarthms.hr.api.HrRequests.UpdateDecisionRequest;
import vn.edu.ptit.smarthms.hr.domain.EmployeeRepository;
import vn.edu.ptit.smarthms.hr.domain.EmployeeStatusHistoryRepository;
import vn.edu.ptit.smarthms.hr.domain.IntegrationOutboxRepository;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecision;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecisionRepository;
import vn.edu.ptit.smarthms.hr.service.PersonnelDecisionService;

@ExtendWith(MockitoExtension.class)
class PersonnelDecisionCrudTest {

  private static final UUID EMP_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID DEC_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

  @Mock
  PersonnelDecisionRepository decisions;

  @Mock
  EmployeeRepository employees;

  @Mock
  EmployeeStatusHistoryRepository histories;

  @Mock
  IntegrationOutboxRepository outbox;

  @Mock
  ActorResolver actor;

  @InjectMocks
  PersonnelDecisionService service;

  private final Actor hr = new Actor("demo-hr", Set.of("HR_SPECIALIST"));

  private PersonnelDecision decision() {
    PersonnelDecision d = new PersonnelDecision(EMP_ID, "DEC-1", PersonnelDecision.DecisionType.TRANSFER, Instant.now().plusSeconds(3600));
    return d;
  }

  @Test
  void updateDraftDecisionSucceeds() {
    PersonnelDecision d = decision();
    when(decisions.findById(DEC_ID)).thenReturn(Optional.of(d));
    when(decisions.existsByDecisionRef("DEC-2")).thenReturn(false);
    when(decisions.save(any(PersonnelDecision.class))).thenAnswer(inv -> inv.getArgument(0));
    var res = service.updateDecision(hr, DEC_ID, new UpdateDecisionRequest("DEC-2", "TERMINATE", Instant.now().plusSeconds(7200), "updated reason"));
    assertEquals("DEC-2", res.decisionRef());
    assertEquals("updated reason", res.reason());
  }

  @Test
  void updateApprovedDecisionIsBlocked() {
    PersonnelDecision d = decision();
    d.setDecisionStatus(PersonnelDecision.DecisionStatus.APPROVED);
    when(decisions.findById(DEC_ID)).thenReturn(Optional.of(d));
    assertThrows(ConflictException.class, () -> service.updateDecision(hr, DEC_ID, new UpdateDecisionRequest(null, null, null, "x")));
  }

  @Test
  void deleteDraftDecisionSucceeds() {
    PersonnelDecision d = decision();
    when(decisions.findById(DEC_ID)).thenReturn(Optional.of(d));
    service.deleteDecision(hr, DEC_ID);
    verify(decisions).delete(d);
  }

  @Test
  void deleteApprovedDecisionIsBlocked() {
    PersonnelDecision d = decision();
    d.setDecisionStatus(PersonnelDecision.DecisionStatus.APPROVED);
    when(decisions.findById(DEC_ID)).thenReturn(Optional.of(d));
    assertThrows(ConflictException.class, () -> service.deleteDecision(hr, DEC_ID));
  }

  @Test
  void deleteRevokedDecisionIsBlocked() {
    PersonnelDecision d = decision();
    d.setDecisionStatus(PersonnelDecision.DecisionStatus.REVOKED);
    when(decisions.findById(DEC_ID)).thenReturn(Optional.of(d));
    assertThrows(ConflictException.class, () -> service.deleteDecision(hr, DEC_ID));
  }
}
