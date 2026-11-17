/**
 * Mô tả: Dữ liệu client gửi khi thêm chẩn đoán (loại, mã ICD-10, mô tả).
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 08/11/2026
 */
package vn.edu.ptit.smarthms.emr.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.ptit.smarthms.emr.domain.DiagnosisType;

public record CreateDiagnosisRequest(
        @NotNull DiagnosisType diagnosisType,
        @Size(max = 20) String icd10Code,
        @NotBlank @Size(max = 2000) String narrative) {}