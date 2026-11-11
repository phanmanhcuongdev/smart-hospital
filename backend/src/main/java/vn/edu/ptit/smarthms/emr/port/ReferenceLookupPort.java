package vn.edu.ptit.smarthms.emr.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferenceLookupPort {
    record PatientSummary(UUID patientId, String displayName, String identityStatus) {}

    record DepartmentSummary(UUID departmentId, String departmentCode, String displayName,
                             String departmentType, boolean active) {}

    record PractitionerDetails(UUID employeeId, String employeeCode, String fullName,
                               String status, boolean active, boolean hasVerifiedLicense,
                               UUID departmentId, String departmentName) {}

    record PractitionerSummary(UUID employeeId, String employeeCode, String fullName,
                               String status, boolean active, String departmentName,
                               boolean hasVerifiedLicense) {
        public PractitionerSummary(UUID employeeId, String employeeCode, String fullName) {
            this(employeeId, employeeCode, fullName, "ACTIVE", true, null, true);
        }
    }

    record MedicationSummary(UUID medicationId, String displayName) {}

    record UserSummary(UUID userId, String username) {}

    boolean patientExists(UUID patientId);

    boolean practitionerExists(UUID employeeId);

    Optional<PractitionerDetails> getPractitionerDetails(UUID employeeId);

    boolean departmentExists(UUID departmentId);

    boolean isDepartmentActive(UUID departmentId);

    List<DepartmentSummary> listDepartments();

    /** Trả về tên thuốc nếu thuốc tồn tại và đang hoạt động. */
    Optional<String> activeMedicationLabel(UUID medicationId);

    /** Tài khoản người dùng (sys.user_account); tạm thời cho khóa/xác nhận hồ sơ. */
    boolean userExists(UUID userId);

    /** Tra cứu bệnh nhân theo tên (tối đa 50 kết quả). */
    List<PatientSummary> searchPatients(String query);

    List<PractitionerSummary> listPractitioners();

    List<MedicationSummary> listMedications();

    /** TẠM THỜI cho dev; V.2 thay bằng người dùng đăng nhập. */
    List<UserSummary> listUsers();
}
