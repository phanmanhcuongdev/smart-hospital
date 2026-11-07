package vn.edu.ptit.smarthms.emr.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.edu.ptit.smarthms.emr.domain.Addendum;
import vn.edu.ptit.smarthms.emr.domain.ClinicalObservation;
import vn.edu.ptit.smarthms.emr.domain.ClinicalResult;
import vn.edu.ptit.smarthms.emr.domain.Diagnosis;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;
import vn.edu.ptit.smarthms.emr.domain.MedicalOrder;
import vn.edu.ptit.smarthms.emr.domain.MedicalRecord;
import vn.edu.ptit.smarthms.emr.domain.Prescription;
import vn.edu.ptit.smarthms.emr.domain.PrescriptionItem;
import vn.edu.ptit.smarthms.emr.domain.RecordVersion;

/** Response bodies for the EMR module. Patient display is minimized: id only. */
public final class EmrResponses {

    private EmrResponses() {
    }

    public record EncounterResponse(UUID id, UUID patientId, UUID appointmentId,
            UUID attendingEmployeeId, String status, Instant startedAt, Instant completedAt,
            Integer rowVersion, Instant createdAt, Instant updatedAt, String createdBy) {
        public static EncounterResponse from(MedicalEncounter e) {
            return new EncounterResponse(e.getId(), e.getPatientId(), e.getAppointmentId(),
                    e.getAttendingEmployeeId(), e.getStatus().name(), e.getStartedAt(), e.getCompletedAt(),
                    e.getRowVersion(), e.getCreatedAt(), e.getUpdatedAt(), e.getCreatedBy());
        }
    }

    public record ObservationResponse(UUID id, UUID encounterId, String metricCode,
            BigDecimal numericValue, String textValue, String unitCode, Instant observedAt,
            String sourceType, String sourceRef, String quality, Instant createdAt, String createdBy) {
        public static ObservationResponse from(ClinicalObservation o) {
            return new ObservationResponse(o.getId(), o.getEncounterId(), o.getMetricCode(),
                    o.getNumericValue(), o.getTextValue(), o.getUnitCode(), o.getObservedAt(),
                    o.getSourceType().name(), null, o.getQuality().name(), o.getCreatedAt(), o.getCreatedBy());
        }
    }

    public record DiagnosisResponse(UUID id, UUID encounterId, String type, String icd10Code,
            String narrative, String status, UUID confirmedBy, Instant createdAt, String createdBy) {
        public static DiagnosisResponse from(Diagnosis d) {
            return new DiagnosisResponse(d.getId(), d.getEncounterId(), d.getType().name(),
                    d.getIcd10Code(), d.getNarrative(), d.getStatus().name(), d.getConfirmedBy(),
                    d.getCreatedAt(), d.getCreatedBy());
        }
    }

    public record OrderResponse(UUID id, UUID encounterId, UUID orderedBy, String type, String status,
            String externalOrderRef, Instant orderedAt, Instant createdAt, String createdBy) {
        public static OrderResponse from(MedicalOrder o) {
            return new OrderResponse(o.getId(), o.getEncounterId(), o.getOrderedBy(), o.getType().name(),
                    o.getStatus().name(), o.getExternalOrderRef(), o.getOrderedAt(), o.getCreatedAt(),
                    o.getCreatedBy());
        }
    }

    public record ResultResponse(UUID id, UUID orderId, String sourceSystem, String sourceResultId,
            String sourceVersion, Instant receivedAt, String resultPayload, Boolean abnormalFlag,
            String quality, boolean duplicate, Instant createdAt) {
        public static ResultResponse from(ClinicalResult r, boolean duplicate) {
            return new ResultResponse(r.getId(), r.getOrderId(), r.getSourceSystem(),
                    r.getSourceResultId(), r.getSourceVersion(), r.getReceivedAt(), r.getResultPayload(),
                    r.getAbnormalFlag(), r.getQuality().name(), duplicate, r.getCreatedAt());
        }
    }

    public record PrescriptionItemResponse(Integer lineNo, UUID medicationId, String medicationLabel,
            String dose, String route, String frequency, Integer daysCount, BigDecimal quantity,
            String unitCode) {
        public static PrescriptionItemResponse from(PrescriptionItem i) {
            return new PrescriptionItemResponse(i.getId().getLineNo(), i.getMedicationId(),
                    i.getMedicationLabel(), i.getDose(), i.getRoute(), i.getFrequency(),
                    i.getDaysCount(), i.getQuantity(), i.getUnitCode());
        }
    }

    public record PrescriptionResponse(UUID id, UUID encounterId, UUID prescriberId, String status,
            Instant issuedAt, List<PrescriptionItemResponse> items, List<String> allergyWarnings,
            Instant createdAt, String createdBy) {
    }

    public record RecordResponse(UUID id, UUID encounterId, String status, Integer currentVersion,
            UUID lockedBy, Instant lockedAt, Integer rowVersion, Instant createdAt, String createdBy) {
        public static RecordResponse from(MedicalRecord r) {
            return new RecordResponse(r.getId(), r.getEncounterId(), r.getStatus().name(),
                    r.getCurrentVersion(), r.getLockedBy(), r.getLockedAt(), r.getRowVersion(),
                    r.getCreatedAt(), r.getCreatedBy());
        }
    }

    public record RecordVersionResponse(Integer versionNo, String sha256Hex, Instant versionCreatedAt,
            String createdBy) {
        public static RecordVersionResponse from(RecordVersion v) {
            return new RecordVersionResponse(v.getVersionNo(), v.getSha256Hex(),
                    v.getVersionCreatedAt(), v.getCreatedBy());
        }
    }

    public record AddendumResponse(UUID id, UUID recordId, String reason, String content,
            String confirmationStatus, Instant confirmedAt, UUID confirmedBy,
            Instant createdAt, String createdBy) {
        public static AddendumResponse from(Addendum a) {
            return new AddendumResponse(a.getId(), a.getRecordId(), a.getReason(), a.getContent(),
                    a.getConfirmationStatus().name(), a.getConfirmedAt(), a.getConfirmedBy(),
                    a.getCreatedAt(), a.getCreatedBy());
        }
    }

    public record EncounterDetailResponse(EncounterResponse encounter,
            List<ObservationResponse> observations, List<DiagnosisResponse> diagnoses,
            List<OrderResponse> orders, List<PrescriptionResponse> prescriptions,
            RecordResponse record) {
    }
}
