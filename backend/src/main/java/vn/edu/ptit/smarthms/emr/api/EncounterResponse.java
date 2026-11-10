package vn.edu.ptit.smarthms.emr.api;

import java.time.OffsetDateTime;
import java.util.UUID;
import vn.edu.ptit.smarthms.emr.domain.MedicalEncounter;

public record EncounterResponse(UUID encounterId, UUID patientId, UUID appointmentId,
                                UUID attendingEmployeeId, String status,
                                OffsetDateTime startedAt, OffsetDateTime completedAt,
                                OffsetDateTime createdAt) {
    public static EncounterResponse from(MedicalEncounter e) {
        return new EncounterResponse(e.getEncounterId(), e.getPatientId(), e.getAppointmentId(),
                e.getAttendingEmployeeId(), e.getStatus().name(), e.getStartedAt(),
                e.getCompletedAt(), e.getCreatedAt());
    }
}