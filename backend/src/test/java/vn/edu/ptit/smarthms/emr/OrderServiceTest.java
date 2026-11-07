package vn.edu.ptit.smarthms.emr;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.emr.api.EmrRequests;
import vn.edu.ptit.smarthms.emr.domain.ClinicalResult;
import vn.edu.ptit.smarthms.emr.domain.ClinicalResultRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounterRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrder;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrderRepository;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecordRepository;
import vn.edu.ptit.smarthms.emr.service.OrderService;
import vn.edu.ptit.smarthms.emr.service.PractitionerPort;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private MedicalEncounterRepository encounters;

    @Mock
    private MedicalOrderRepository orders;

    @Mock
    private ClinicalResultRepository results;

    @Mock
    private MedicalRecordRepository records;

    @Mock
    private PractitionerPort practitionerPort;

    @InjectMocks
    private OrderService service;

    private static final UUID ENCOUNTER_ID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111");
    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-2222-2222-2222-222222222222");
    private static final UUID PATIENT_ID = UUID.fromString("aaaaaaaa-3333-3333-3333-333333333333");
    private static final UUID DOCTOR_ID = UUID.fromString("aaaaaaaa-4444-4444-4444-444444444444");
    private static final UUID OTHER_PATIENT_ID = UUID.fromString("aaaaaaaa-9999-9999-9999-999999999999");

    private static final Actor CLS = new Actor("demo-cls", Set.of("CLS_TECHNICIAN"));

    private MedicalEncounter encounter() {
        MedicalEncounter medicalEncounter = new MedicalEncounter(PATIENT_ID, null, DOCTOR_ID);
        medicalEncounter.setStatus(MedicalEncounter.Status.IN_PROGRESS);
        return medicalEncounter;
    }

    private MedicalOrder order() {
        MedicalOrder medicalOrder = new MedicalOrder(ENCOUNTER_ID, DOCTOR_ID, MedicalOrder.Type.LAB, null);
        medicalOrder.setStatus(MedicalOrder.Status.ORDERED);
        return medicalOrder;
    }

    private EmrRequests.ResultRequest resultRequest(UUID patientId) {
        return new EmrRequests.ResultRequest(patientId, "LIS", "RES-1", "v1", "{\"value\": 5}", true);
    }

    private void stubOrderAndEncounter() {
        when(orders.findById(ORDER_ID)).thenReturn(Optional.of(order()));
        when(encounters.findById(ENCOUNTER_ID)).thenReturn(Optional.of(encounter()));
        lenient().when(records.findByEncounterId(ENCOUNTER_ID)).thenReturn(Optional.empty());
    }

    @Test
    void receiveResultReturnsExistingOnDuplicateSource() {
        stubOrderAndEncounter();
        ClinicalResult existing = new ClinicalResult(ORDER_ID, "LIS", "RES-1", "v1", "{\"value\": 5}", true, ClinicalResult.Quality.VALID);
        when(results.findBySourceSystemAndSourceResultIdAndSourceVersion("LIS", "RES-1", "v1"))
                .thenReturn(Optional.of(existing));
        var response = service.receiveResult(CLS, ORDER_ID, resultRequest(PATIENT_ID));
        assertTrue(response.duplicate());
        verify(results, never()).save(any());
        verify(orders, never()).save(any());
    }

    @Test
    void receiveResultQuarantinesWrongPatientResult() {
        stubOrderAndEncounter();
        when(results.findBySourceSystemAndSourceResultIdAndSourceVersion("LIS", "RES-1", "v1"))
                .thenReturn(Optional.empty());
        when(results.save(any(ClinicalResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.receiveResult(CLS, ORDER_ID, resultRequest(OTHER_PATIENT_ID));
        assertFalse(response.duplicate());
        ArgumentCaptor<ClinicalResult> captor = ArgumentCaptor.forClass(ClinicalResult.class);
        verify(results).save(captor.capture());
        assertEquals(ClinicalResult.Quality.QUARANTINED, captor.getValue().getQuality());
        verify(orders, never()).save(any());
    }

    @Test
    void receiveResultCompletesOrderForValidResult() {
        stubOrderAndEncounter();
        when(results.findBySourceSystemAndSourceResultIdAndSourceVersion("LIS", "RES-1", "v1"))
                .thenReturn(Optional.empty());
        when(results.save(any(ClinicalResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orders.save(any(MedicalOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.receiveResult(CLS, ORDER_ID, resultRequest(PATIENT_ID));
        assertFalse(response.duplicate());
        ArgumentCaptor<ClinicalResult> resultCaptor = ArgumentCaptor.forClass(ClinicalResult.class);
        verify(results).save(resultCaptor.capture());
        assertEquals(ClinicalResult.Quality.VALID, resultCaptor.getValue().getQuality());
        ArgumentCaptor<MedicalOrder> orderCaptor = ArgumentCaptor.forClass(MedicalOrder.class);
        verify(orders).save(orderCaptor.capture());
        assertEquals(MedicalOrder.Status.COMPLETED, orderCaptor.getValue().getStatus());
    }
}
