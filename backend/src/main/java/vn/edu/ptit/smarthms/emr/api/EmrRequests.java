package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request bodies for the EMR module. Enums arrive as their schema strings. */
public final class EmrRequests {

    private EmrRequests() {
    }

    public record OpenEncounterRequest(
            @NotNull UUID patientId,
            UUID appointmentId,
            @NotNull UUID attendingEmployeeId) {
    }

    public record VitalsRequest(
            @NotBlank String metricCode,
            BigDecimal numericValue,
            String textValue,
            String unitCode,
            @NotNull Instant observedAt) {
    }

    public record DiagnosisRequest(
            @NotBlank String type,
            String icd10Code,
            @NotBlank String narrative,
            @NotBlank String status) {
    }

    public record OrderRequest(
            @NotBlank String type,
            String externalOrderRef) {
    }

    public record ResultRequest(
            @NotNull UUID patientId,
            @NotBlank String sourceSystem,
            @NotBlank String sourceResultId,
            @NotBlank String sourceVersion,
            @NotBlank String resultPayload,
            Boolean abnormalFlag) {
    }

    public record PrescriptionItemRequest(
            @NotNull UUID medicationId,
            @NotBlank String dose,
            @NotBlank String route,
            @NotBlank String frequency,
            @NotNull @Positive Integer daysCount,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
            @NotBlank String unitCode) {
    }

    public record PrescriptionRequest(
            @NotEmpty @Valid List<PrescriptionItemRequest> items) {
    }

    public record LockRequest(
            @NotNull Integer expectedRowVersion) {
    }

    public record AddendumRequest(
            @NotBlank String reason,
            @NotBlank String content) {
    }
}
