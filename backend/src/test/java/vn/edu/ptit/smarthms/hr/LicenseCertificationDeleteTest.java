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
import vn.edu.ptit.smarthms.hr.domain.EmployeeRepository;
import vn.edu.ptit.smarthms.hr.domain.LicenseCertification;
import vn.edu.ptit.smarthms.hr.domain.LicenseCertificationRepository;
import vn.edu.ptit.smarthms.hr.service.LicenseCertificationService;

@ExtendWith(MockitoExtension.class)
class LicenseCertificationDeleteTest {

  private static final UUID EMP_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID CERT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

  @Mock
  LicenseCertificationRepository certifications;

  @Mock
  EmployeeRepository employees;

  @Mock
  ActorResolver actor;

  @InjectMocks
  LicenseCertificationService service;

  private final Actor hr = new Actor("demo-hr", Set.of("HR_SPECIALIST"));

  @Test
  void deleteSucceeds() {
    LicenseCertification cert = new LicenseCertification(EMP_ID, "CCHN", "SRC-1");
    when(certifications.findById(CERT_ID)).thenReturn(Optional.of(cert));
    service.deleteCertification(hr, CERT_ID);
    verify(certifications).delete(cert);
  }

  @Test
  void deleteMissingCertificationThrows404() {
    when(certifications.findById(CERT_ID)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> service.deleteCertification(hr, CERT_ID));
  }
}
