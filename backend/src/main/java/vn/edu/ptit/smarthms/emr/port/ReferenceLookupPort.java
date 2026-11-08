package vn.edu.ptit.smarthms.emr.port;

import java.util.Optional;
import java.util.UUID;

public interface ReferenceLookupPort {
    boolean patientExists(UUID patientId);
    boolean practitionerExists(UUID employeeId);
    /** Trả về tên thuốc nếu thuốc tồn tại và đang hoạt động. */
    Optional<String> activeMedicationLabel(UUID medicationId);
}