/**
 * Mô tả: Dữ liệu client gửi khi tạo lần khám (bệnh nhân, lịch hẹn tùy chọn, bác sĩ phụ trách) kèm kiểm tra bắt buộc.
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 08/11/2026
 */
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
