package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisType;

public record CreateDiagnosisRequest(
        @NotNull DiagnosisType diagnosisType,
        @Size(max = 20) String icd10Code,
        @NotBlank @Size(max = 2000) String narrative) {}