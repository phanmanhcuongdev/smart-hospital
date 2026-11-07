package vn.edu.ptit.smarthms.emr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.ptit.smarthms.common.error.ForbiddenException;
import vn.edu.ptit.smarthms.common.error.ValidationException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.api.EmrRequests;
import vn.edu.ptit.smarthms.emr.domain.AllergyFixture;
import vn.edu.ptit.smarthms.emr.domain.AllergyFixtureRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecordRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicationFixture;
import vn.edu.ptit.smarthms.emr.domain.MedicationFixtureRepository;
import vn.edu.ptit.smarthms.emr.domain.Prescription;
import vn.edu.ptit.smarthms.emr.domain.PrescriptionItemRepository;
import vn.edu.ptit.smarthms.emr.domain.PrescriptionRepository;
import vn.edu.ptit.smarthms.emr.service.PrescriptionService;
import vn.edu.ptit.smarthms.emr.service.PractitionerPort;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private MedicalEncounterRepository encounters;

    @Mock
    private PrescriptionRepository prescriptions;

    @Mock
    private PrescriptionItemRepository items;

    @Mock
    private MedicationFixtureRepository medications;

    @Mock
    private AllergyFixtureRepository allergies;

    @Mock
    private MedicalRecordRepository records;

    @Mock
    private PractitionerPort practitionerPort;

    @InjectMocks
    private PrescriptionService service;

    private static final UUID ENCOUNTER_ID = UUID.fromString("cccccccc-1111-1111-1111-111111111111");
    private static final UUID PATIENT_ID = UUID.fromString("cccccccc-2222-2222-2222-222222222222");
    private static final UUID DOCTOR_ID = UUID.fromString("cccccccc-3333-3333-3333-333333333333");
    private static final UUID MEDICATION_ID = UUID.fromString("cccccccc-4444-4444-4444-444444444444");

    private static final Actor DOCTOR = new Actor("EMP-DEMO-001", Set.of("DOCTOR"));

    private MedicalEncounter encounter() {
        MedicalEncounter medicalEncounter = new MedicalEncounter(PATIENT_ID, null, DOCTOR_ID);
        medicalEncounter.setStatus(MedicalEncounter.Status.IN_PROGRESS);
        return medicalEncounter;
    }

    private MedicationFixture medication(boolean active) {
        MedicationFixture fixture = mock(MedicationFixture.class);
        lenient().when(fixture.isActive()).thenReturn(active);
        lenient().when(fixture.getDisplayName()).thenReturn("Synthetic Medication");
        return fixture;
    }

    private AllergyFixture allergy(String allergen, AllergyFixture.Status status) {
        AllergyFixture fixture = mock(AllergyFixture.class);
        lenient().when(fixture.getAllergen()).thenReturn(allergen);
        lenient().when(fixture.getStatus()).thenReturn(status);
        return fixture;
    }

    private EmrRequests.PrescriptionRequest prescriptionRequest() {
        return new EmrRequests.PrescriptionRequest(List.of(new EmrRequests.PrescriptionItemRequest(
                MEDICATION_ID, "500 mg", "PO", "BID", 5, new BigDecimal("10"), "TAB")));
    }

    private void stubBase(boolean cchnValid) {
        when(encounters.findById(ENCOUNTER_ID)).thenReturn(Optional.of(encounter()));
        lenient().when(records.findByEncounterId(ENCOUNTER_ID)).thenReturn(Optional.empty());
        when(practitionerPort.employeeIdForCode("EMP-DEMO-001")).thenReturn(Optional.of(DOCTOR_ID));
        when(practitionerPort.isActiveEmployee(DOCTOR_ID)).thenReturn(true);
        when(practitionerPort.hasValidCchn(eq(DOCTOR_ID), any(LocalDate.class))).thenReturn(cchnValid);
        MedicationFixture fixture = medication(true);
        lenient().when(medications.findById(MEDICATION_ID)).thenReturn(Optional.of(fixture));
        lenient().when(prescriptions.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(items.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createPrescriptionWithoutAllergyWarning() {
        stubBase(true);
        AllergyFixture allergyFixture = allergy("Synthetic Medication", AllergyFixture.Status.NONE_CONFIRMED);
        when(allergies.findByPatientId(PATIENT_ID)).thenReturn(List.of(allergyFixture));
        var response = service.createPrescription(DOCTOR, ENCOUNTER_ID, prescriptionRequest());
        assertTrue(response.allergyWarnings().isEmpty());
    }

    @Test
    void createPrescriptionRejectsWithoutValidCchn() {
        stubBase(false);
        assertThrows(ForbiddenException.class, () -> service.createPrescription(DOCTOR, ENCOUNTER_ID, prescriptionRequest()));
        verify(prescriptions, never()).save(any());
    }

    @Test
    void createPrescriptionBlocksKnownAllergy() {
        stubBase(true);
        AllergyFixture allergyFixture = allergy("Synthetic Medication", AllergyFixture.Status.HAS_ALLERGY);
        when(allergies.findByPatientId(PATIENT_ID)).thenReturn(List.of(allergyFixture));
        assertThrows(ValidationException.class, () -> service.createPrescription(DOCTOR, ENCOUNTER_ID, prescriptionRequest()));
        verify(prescriptions, never()).save(any());
    }

    @Test
    void createPrescriptionWarnsOnUnknownAllergyStatus() {
        stubBase(true);
        AllergyFixture allergyFixture = allergy("Synthetic Medication", AllergyFixture.Status.UNKNOWN);
        when(allergies.findByPatientId(PATIENT_ID)).thenReturn(List.of(allergyFixture));
        var response = service.createPrescription(DOCTOR, ENCOUNTER_ID, prescriptionRequest());
        assertFalse(response.allergyWarnings().isEmpty());
        assertTrue(response.allergyWarnings().get(0).contains("UNKNOWN"));
    }

    @Test
    void createPrescriptionWarnsWhenNoAllergyInformation() {
        stubBase(true);
        when(allergies.findByPatientId(PATIENT_ID)).thenReturn(List.of());
        var response = service.createPrescription(DOCTOR, ENCOUNTER_ID, prescriptionRequest());
        assertFalse(response.allergyWarnings().isEmpty());
    }

    @Test
    void createPrescriptionRejectsInactiveMedication() {
        stubBase(true);
        MedicationFixture fixture = medication(false);
        when(medications.findById(MEDICATION_ID)).thenReturn(Optional.of(fixture));
        assertThrows(ValidationException.class, () -> service.createPrescription(DOCTOR, ENCOUNTER_ID, prescriptionRequest()));
        verify(prescriptions, never()).save(any());
    }
}
