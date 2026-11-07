package vn.edu.ptit.smarthms.emr.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Port through which EMR reads practitioner facts owned by HR (BR-015, BR-016). */
/** EMR never queries HR tables directly. The HR module provides the adapter. */
public interface PractitionerPort {

    /** Employee id for a principal name, which is the employee code in this project. */
    Optional<UUID> employeeIdForCode(String employeeCode);

    /** True when the employee exists and has status ACTIVE. */
    boolean isActiveEmployee(UUID employeeId);

    /** True when the employee holds a VERIFIED CCHN that is not expired. */
    boolean hasValidCchn(UUID employeeId, LocalDate today);

    /** Computed CCHN validity: VALID, EXPIRING_SOON, EXPIRED, UNVERIFIED or REJECTED. */
    String cchnValidity(UUID employeeId, LocalDate today);
}
