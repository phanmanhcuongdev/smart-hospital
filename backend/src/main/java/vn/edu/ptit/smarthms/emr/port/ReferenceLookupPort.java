package vn.edu.ptit.smarthms.emr.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferenceLookupPort {
    record PatientSummary(UUID patientId, String displayName, String identityStatus) {}

    boolean patientExists(UUID patientId);
    boolean practitionerExists(UUID employeeId);
    /** Trả về tên thuốc nếu thuốc tồn tại và đang hoạt động. */
    Optional<String> activeMedicationLabel(UUID medicationId);
    /** Tài khoản người dùng (sys.user_account); tạm thời cho khóa/xác nhận hồ sơ. */
    boolean userExists(UUID userId);
    /** Tra cứu bệnh nhân theo tên (tối đa 50 kết quả). */
    List<PatientSummary> searchPatients(String query);
        record PractitionerSummary(UUID employeeId, String employeeCode, String fullName) {}

    List<PractitionerSummary> listPractitioners();
        record MedicationSummary(UUID medicationId, String displayName) {}

    List<MedicationSummary> listMedications();
        record UserSummary(UUID userId, String username) {}

    /** TẠM THỜI cho dev; V.2 thay bằng người dùng đăng nhập. */
    List<UserSummary> listUsers();
}