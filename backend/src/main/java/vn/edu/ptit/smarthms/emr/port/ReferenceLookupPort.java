package vn.edu.ptit.smarthms.emr.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferenceLookupPort {
    record PatientSummary(UUID patientId, String displayName, String identityStatus) {}

    record PractitionerSummary(UUID employeeId, String employeeCode, String fullName) {}

    record MedicationSummary(UUID medicationId, String displayName) {}

    record UserSummary(UUID userId, String username) {}

    record AllergyFact(String allergyStatus, String allergen) {}

    boolean patientExists(UUID patientId);

    /** VERIFIED / UNVERIFIED / FLAGGED; rỗng nếu không có bệnh nhân. */
    Optional<String> patientIdentityStatus(UUID patientId);

    boolean practitionerExists(UUID employeeId);

    /** Nhân viên có trạng thái ACTIVE (IV7 M-20; chứng chỉ hành nghề còn chờ OPEN-03). */
    boolean practitionerActive(UUID employeeId);

    /** Trả về tên thuốc nếu thuốc tồn tại và đang hoạt động. */
    Optional<String> activeMedicationLabel(UUID medicationId);

    /** Tài khoản người dùng (sys.user_account); tạm thời cho khóa/xác nhận hồ sơ. */
    boolean userExists(UUID userId);

    List<PatientSummary> searchPatients(String query);

    List<PractitionerSummary> listPractitioners();

    List<MedicationSummary> listMedications();

    /** TẠM THỜI cho dev; V.2 thay bằng người dùng đăng nhập. */
    List<UserSummary> listUsers();

    List<AllergyFact> allergiesOf(UUID patientId);
}