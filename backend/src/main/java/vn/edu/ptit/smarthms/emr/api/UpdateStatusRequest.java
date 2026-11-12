package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;

public record UpdateStatusRequest(
        @NotNull EncounterStatus status,
        UUID attendingEmployeeId,
        UUID departmentId) {
    public UpdateStatusRequest(EncounterStatus status) {
        this(status, null, null);
    }
}
