package vn.edu.ptit.smarthms.emr;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.error.ForbiddenException;
import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.api.EmrRequests;
import vn.edu.ptit.smarthms.emr.domain.ClinicalObservation;
import vn.edu.ptit.smarthms.emr.domain.ClinicalObservationRepository;
import vn.edu.ptit.smarthms.emr.domain.Diagnosis;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrderRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecordRepository;
import vn.edu.ptit.smarthms.emr.domain.PatientFixture;
import vn.edu.ptit.smarthms.emr.domain.PatientFixtureRepository;
import vn.edu.ptit.smarthms.emr.domain.PrescriptionRepository;
import vn.edu.ptit.smarthms.emr.service.EncounterService;
import vn.edu.ptit.smarthms.emr.service.PractitionerPort;

@ExtendWith(MockitoExtension.class)
class EncounterServiceTest {

    @Mock
    private MedicalEncounterRepository encounters;

    @Mock
    private ClinicalObservationRepository observations;

    @Mock
    private DiagnosisRepository diagnoses;

    @Mock
    private MedicalRecordRepository records;

    @Mock
    private MedicalOrderRepository orders;

    @Mock
    private PrescriptionRepository prescriptions;

    @Mock
    private PatientFixtureRepository patients;

    @Mock
    private PractitionerPort practitionerPort;

    @InjectMocks
    private EncounterService service;

    private static final UUID PATIENT_ID = UUID.fromString("bbbbbbbb-1111-1111-1111-111111111111");
    private static final UUID DOCTOR_ID = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222");
    private static final UUID ENCOUNTER_ID = UUID.fromString("bbbbbbbb-3333-3333-3333-333333333333");

    private static final Actor RECEPTION = new Actor("demo-reception", Set.of("RECEPTION"));
    private static final Actor NURSE = new Actor("demo-nurse", Set.of("NURSE"));
    private static final Actor DOCTOR = new Actor("EMP-DEMO-001", Set.of("DOCTOR"));

    private PatientFixture patient(PatientFixture.IdentityStatus identityStatus) {
        PatientFixture fixture = mock(PatientFixture.class);
        when(fixture.getIdentityStatus()).thenReturn(identityStatus);
        return fixture;
    }

    private MedicalEncounter encounter() {
        MedicalEncounter medicalEncounter = new MedicalEncounter(PATIENT_ID, null, DOCTOR_ID);
        medicalEncounter.setStatus(MedicalEncounter.Status.IN_PROGRESS);
        return medicalEncounter;
    }

    @Test
    void openEncounterCreatesWaitingEncounterForVerifiedPatient() {
        PatientFixture fixture = patient(PatientFixture.IdentityStatus.VERIFIED);
        when(patients.findById(PATIENT_ID)).thenReturn(Optional.of(fixture));
        when(practitionerPort.isActiveEmployee(DOCTOR_ID)).thenReturn(true);
        when(encounters.save(any(MedicalEncounter.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.openEncounter(RECEPTION, new EmrRequests.OpenEncounterRequest(PATIENT_ID, null, DOCTOR_ID));
        ArgumentCaptor<MedicalEncounter> captor = ArgumentCaptor.forClass(MedicalEncounter.class);
        verify(encounters).save(captor.capture());
        assertEquals(MedicalEncounter.Status.WAITING, captor.getValue().getStatus());
        assertEquals(PATIENT_ID, captor.getValue().getPatientId());
        assertEquals(DOCTOR_ID, captor.getValue().getAttendingEmployeeId());
    }

    @Test
    void openEncounterRejectsUnverifiedPatient() {
        PatientFixture fixture = patient(PatientFixture.IdentityStatus.UNVERIFIED);
        when(patients.findById(PATIENT_ID)).thenReturn(Optional.of(fixture));
        assertThrows(ValidationException.class, () -> service.openEncounter(RECEPTION, new EmrRequests.OpenEncounterRequest(PATIENT_ID, null, DOCTOR_ID)));
        verify(encounters, never()).save(any());
    }

    @Test
    void openEncounterRejectsFlaggedPatient() {
        PatientFixture fixture = patient(PatientFixture.IdentityStatus.FLAGGED);
        when(patients.findById(PATIENT_ID)).thenReturn(Optional.of(fixture));
        assertThrows(ForbiddenException.class, () -> service.openEncounter(RECEPTION, new EmrRequests.OpenEncounterRequest(PATIENT_ID, null, DOCTOR_ID)));
        verify(encounters, never()).save(any());
    }

    @Test
    void openEncounterRejectsInactiveAttendingEmployee() {
        PatientFixture fixture = patient(PatientFixture.IdentityStatus.VERIFIED);
        when(patients.findById(PATIENT_ID)).thenReturn(Optional.of(fixture));
        when(practitionerPort.isActiveEmployee(DOCTOR_ID)).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> service.openEncounter(RECEPTION, new EmrRequests.OpenEncounterRequest(PATIENT_ID, null, DOCTOR_ID)));
        verify(encounters, never()).save(any());
    }

    @Test
    void recordVitalsByNurseSavesObservation() {
        when(encounters.findById(ENCOUNTER_ID)).thenReturn(Optional.of(encounter()));
        lenient().when(records.findByEncounterId(ENCOUNTER_ID)).thenReturn(Optional.empty());
        when(observations.save(any(ClinicalObservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.recordVitals(NURSE, ENCOUNTER_ID, new EmrRequests.VitalsRequest("BP_SYS", new BigDecimal("120"), null, "mmHg", Instant.now()));
        ArgumentCaptor<ClinicalObservation> captor = ArgumentCaptor.forClass(ClinicalObservation.class);
        verify(observations).save(captor.capture());
        assertEquals("BP_SYS", captor.getValue().getMetricCode());
        assertEquals(new BigDecimal("120"), captor.getValue().getNumericValue());
    }

    @Test
    void recordVitalsRejectsNonNurse() {
        lenient().when(encounters.findById(ENCOUNTER_ID)).thenReturn(Optional.of(encounter()));
        assertThrows(ForbiddenException.class, () -> service.recordVitals(DOCTOR, ENCOUNTER_ID, new EmrRequests.VitalsRequest("BP_SYS", new BigDecimal("120"), null, "mmHg", Instant.now())));
        verify(observations, never()).save(any());
    }

    @Test
    void recordDiagnosisConfirmsWithIcd10() {
        when(encounters.findById(ENCOUNTER_ID)).thenReturn(Optional.of(encounter()));
        lenient().when(records.findByEncounterId(ENCOUNTER_ID)).thenReturn(Optional.empty());
        when(practitionerPort.employeeIdForCode("EMP-DEMO-001")).thenReturn(Optional.of(DOCTOR_ID));
        when(diagnoses.findByEncounterIdAndType(ENCOUNTER_ID, Diagnosis.Type.PRIMARY)).thenReturn(Optional.empty());
        when(diagnoses.save(any(Diagnosis.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.recordDiagnosis(DOCTOR, ENCOUNTER_ID, new EmrRequests.DiagnosisRequest("PRIMARY", "A00", "Suspected cholera", "CONFIRMED"));
        ArgumentCaptor<Diagnosis> captor = ArgumentCaptor.forClass(Diagnosis.class);
        verify(diagnoses).save(captor.capture());
        assertEquals(Diagnosis.Type.PRIMARY, captor.getValue().getType());
        assertEquals(Diagnosis.Status.CONFIRMED, captor.getValue().getStatus());
    }

    @Test
    void recordDiagnosisRejectsConfirmedWithoutIcd10() {
        when(encounters.findById(ENCOUNTER_ID)).thenReturn(Optional.of(encounter()));
        when(practitionerPort.employeeIdForCode("EMP-DEMO-001")).thenReturn(Optional.of(DOCTOR_ID));
        lenient().when(records.findByEncounterId(ENCOUNTER_ID)).thenReturn(Optional.empty());
        lenient().when(diagnoses.findByEncounterIdAndType(ENCOUNTER_ID, Diagnosis.Type.PRIMARY)).thenReturn(Optional.empty());
        assertThrows(ValidationException.class, () -> service.recordDiagnosis(DOCTOR, ENCOUNTER_ID, new EmrRequests.DiagnosisRequest("PRIMARY", null, "Suspected cholera", "CONFIRMED")));
        verify(diagnoses, never()).save(any());
    }

    @Test
    void recordDiagnosisRejectsSecondConfirmedPrimary() {
        when(encounters.findById(ENCOUNTER_ID)).thenReturn(Optional.of(encounter()));
        when(practitionerPort.employeeIdForCode("EMP-DEMO-001")).thenReturn(Optional.of(DOCTOR_ID));
        Diagnosis existing = new Diagnosis(ENCOUNTER_ID, Diagnosis.Type.PRIMARY, "A00", "Cholera");
        existing.setStatus(Diagnosis.Status.CONFIRMED);
        when(diagnoses.findByEncounterIdAndType(ENCOUNTER_ID, Diagnosis.Type.PRIMARY)).thenReturn(Optional.of(existing));
        assertThrows(ConflictException.class, () -> service.recordDiagnosis(DOCTOR, ENCOUNTER_ID, new EmrRequests.DiagnosisRequest("PRIMARY", "B00", "Herpesviral infection", "CONFIRMED")));
        verify(diagnoses, never()).save(any());
    }
}
