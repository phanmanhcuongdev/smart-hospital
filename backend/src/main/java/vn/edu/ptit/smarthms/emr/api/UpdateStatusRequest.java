package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.constraints.NotNull;
import vn.edu.ptit.smarthms.emr.domain.EncounterStatus;

public record UpdateStatusRequest(@NotNull EncounterStatus status) {}