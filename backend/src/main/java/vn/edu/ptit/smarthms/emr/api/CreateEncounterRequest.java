package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateEncounterRequest(
        @NotNull UUID patientId,
        UUID appointmentId,
        @NotNull UUID attendingEmployeeId,
        UUID departmentId) {
    public CreateEncounterRequest(UUID patientId, UUID appointmentId, UUID attendingEmployeeId) {
        this(patientId, appointmentId, attendingEmployeeId, null);
    }
}
