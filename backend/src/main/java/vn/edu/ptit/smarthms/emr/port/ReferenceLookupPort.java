package vn.edu.ptit.smarthms.emr.port;

import java.util.UUID;

public interface ReferenceLookupPort {
    boolean patientExists(UUID patientId);
    boolean practitionerExists(UUID employeeId);
}