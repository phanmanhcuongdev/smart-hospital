package vn.edu.ptit.smarthms.emr;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.ptit.smarthms.common.error.ConflictException;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.api.EmrRequests;
import vn.edu.ptit.smarthms.emr.domain.Addendum;
import vn.edu.ptit.smarthms.emr.domain.AddendumRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecordRepository;
import vn.edu.ptit.smarthms.emr.domain.RecordVersion;
import vn.edu.ptit.smarthms.emr.domain.RecordVersionRepository;
import vn.edu.ptit.smarthms.emr.service.MedicalRecordService;
import vn.edu.ptit.smarthms.emr.service.PractitionerPort;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock
    private MedicalEncounterRepository encounters;

    @Mock
    private MedicalRecordRepository records;

    @Mock
    private RecordVersionRepository versions;

    @Mock
    private AddendumRepository addenda;

    @Mock
    private PractitionerPort practitionerPort;

    @InjectMocks
    private MedicalRecordService service;

    private static final UUID ENCOUNTER_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private static final UUID RECORD_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID DOCTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PATIENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final Actor RECORDS = new Actor("demo-records", Set.of("RECORDS_MANAGER", "DOCTOR"));

    private static MedicalRecord record(MedicalRecord.Status status) {
        MedicalRecord medicalRecord = new MedicalRecord(ENCOUNTER_ID);
        medicalRecord.setStatus(status);
        medicalRecord.setCurrentVersion(1);
        return medicalRecord;
    }

    private static MedicalEncounter encounter() {
        MedicalEncounter encounter = new MedicalEncounter(PATIENT_ID, null, DOCTOR_ID);
        encounter.setStatus(MedicalEncounter.Status.IN_PROGRESS);
        return encounter;
    }

    @BeforeEach
    void setUp() {
        lenient().when(encounters.findById(ENCOUNTER_ID)).thenReturn(Optional.of(encounter()));
        lenient().when(practitionerPort.employeeIdForCode("demo-records")).thenReturn(Optional.of(DOCTOR_ID));
        lenient().when(practitionerPort.employeeIdForCode("EMP-DEMO-001")).thenReturn(Optional.of(DOCTOR_ID));
    }

    @Test
    void submitForSignatureRejectsStaleRowVersion() {
        when(records.findById(RECORD_ID)).thenReturn(Optional.of(record(MedicalRecord.Status.DRAFT)));
        assertThrows(ConflictException.class, () -> service.submitForSignature(RECORDS, RECORD_ID, new EmrRequests.LockRequest(99)));
        verify(versions, never()).save(any());
    }

    @Test
    void submitForSignatureCreatesNewVersion() {
        when(records.findById(RECORD_ID)).thenReturn(Optional.of(record(MedicalRecord.Status.DRAFT)));
        when(records.save(any(MedicalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versions.save(any(RecordVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.submitForSignature(RECORDS, RECORD_ID, new EmrRequests.LockRequest(1));
        ArgumentCaptor<RecordVersion> versionCaptor = ArgumentCaptor.forClass(RecordVersion.class);
        verify(versions).save(versionCaptor.capture());
        assertEquals(2, versionCaptor.getValue().getVersionNo());
        assertEquals(64, versionCaptor.getValue().getSha256Hex().length());
        ArgumentCaptor<MedicalRecord> recordCaptor = ArgumentCaptor.forClass(MedicalRecord.class);
        verify(records).save(recordCaptor.capture());
        assertEquals(MedicalRecord.Status.PENDING_SIGNATURE, recordCaptor.getValue().getStatus());
        assertEquals(2, recordCaptor.getValue().getCurrentVersion());
    }

    @Test
    void submitForSignatureRejectsNonDraftRecord() {
        when(records.findById(RECORD_ID)).thenReturn(Optional.of(record(MedicalRecord.Status.LOCKED)));
        assertThrows(ConflictException.class, () -> service.submitForSignature(RECORDS, RECORD_ID, new EmrRequests.LockRequest(1)));
        verify(versions, never()).save(any());
    }

    @Test
    void addAddendumRequiresLockedParent() {
        when(records.findById(RECORD_ID)).thenReturn(Optional.of(record(MedicalRecord.Status.DRAFT)));
        assertThrows(ConflictException.class, () -> service.addAddendum(RECORDS, RECORD_ID, new EmrRequests.AddendumRequest("Correction", "Fixed a typo")));
        verify(addenda, never()).save(any());
    }

    @Test
    void addAddendumOnLockedRecordStaysDraft() {
        when(records.findById(RECORD_ID)).thenReturn(Optional.of(record(MedicalRecord.Status.LOCKED)));
        when(addenda.save(any(Addendum.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.addAddendum(RECORDS, RECORD_ID, new EmrRequests.AddendumRequest("Correction", "Fixed a typo"));
        ArgumentCaptor<Addendum> captor = ArgumentCaptor.forClass(Addendum.class);
        verify(addenda).save(captor.capture());
        assertEquals(Addendum.ConfirmationStatus.DRAFT, captor.getValue().getConfirmationStatus());
        assertEquals("Correction", captor.getValue().getReason());
    }

    @Test
    void confirmAddendumSetsConfirmerAndTime() {
        UUID addendumId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID doctorId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        Addendum addendum = new Addendum(RECORD_ID, "Correction", "Fixed a typo");
        when(addenda.findById(addendumId)).thenReturn(Optional.of(addendum));
        lenient().when(records.findById(RECORD_ID)).thenReturn(Optional.of(record(MedicalRecord.Status.LOCKED)));
        when(practitionerPort.employeeIdForCode("EMP-DEMO-001")).thenReturn(Optional.of(doctorId));
        when(addenda.save(any(Addendum.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Actor doctor = new Actor("EMP-DEMO-001", Set.of("DOCTOR"));
        service.confirmAddendum(doctor, addendumId);
        ArgumentCaptor<Addendum> captor = ArgumentCaptor.forClass(Addendum.class);
        verify(addenda).save(captor.capture());
        assertEquals(Addendum.ConfirmationStatus.CONFIRMED, captor.getValue().getConfirmationStatus());
        assertEquals(doctorId, captor.getValue().getConfirmedBy());
        assertNotNull(captor.getValue().getConfirmedAt());
    }
}
