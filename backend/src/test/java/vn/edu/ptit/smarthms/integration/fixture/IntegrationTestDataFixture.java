package vn.edu.ptit.smarthms.integration.fixture;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;
import vn.edu.ptit.smarthms.hr.domain.Employee;
import vn.edu.ptit.smarthms.hr.domain.PersonnelDecision;

public final class IntegrationTestDataFixture {

    private IntegrationTestDataFixture() {}

    // TD-PAT-01
    public static final UUID PAT_TEST_001 = UUID.fromString("11111111-1111-1111-1111-111111110001");
    public static final String PAT_NAME_001 = "Nguyen Van Test";

    // TD-APT-01
    public static final UUID APT_TEST_001 = UUID.fromString("11111111-1111-1111-1111-111111110002");

    // TD-EMR-01
    public static final UUID ENC_TEST_001 = UUID.fromString("11111111-1111-1111-1111-111111110003");

    // Doctors
    public static final UUID DOC_TEST_001_ID = UUID.fromString("22222222-2222-2222-2222-222222220001");
    public static final String DOC_TEST_001_CODE = "DOC-TEST-001";
    public static final UUID DEPT_TEST_001_ID = UUID.fromString("33333333-3333-3333-3333-333333330001");

    // Terminated employee for TD-HR-02 / TD-SYS-05
    public static final UUID EMP_TEST_102_ID = UUID.fromString("22222222-2222-2222-2222-222222220102");
    public static final String EMP_TEST_102_CODE = "EMP-TEST-102";

    // Decisions
    public static final UUID DEC_TEST_001_ID = UUID.fromString("44444444-4444-4444-4444-444444440001");
    public static final String DEC_TEST_001_CODE = "DEC-TEST-001";

    // CLS
    public static final String EV_CLS_001 = "EV-CLS-001";
    public static final String CLS_SOURCE_SYSTEM = "MOCK-CLS-LAB";

    public static ReferenceLookupPort.PractitionerDetails validPractitioner() {
        return new ReferenceLookupPort.PractitionerDetails(
                DOC_TEST_001_ID, DOC_TEST_001_CODE, "Dr. Test Valid",
                "ACTIVE", true, true,
                DEPT_TEST_001_ID, "Khoa Kham Benh"
        );
    }

    public static ReferenceLookupPort.PractitionerDetails expiredLicensePractitioner() {
        return new ReferenceLookupPort.PractitionerDetails(
                DOC_TEST_001_ID, DOC_TEST_001_CODE, "Dr. Test Expired CCHN",
                "ACTIVE", true, false, // hasVerifiedLicense = false
                DEPT_TEST_001_ID, "Khoa Kham Benh"
        );
    }

    public static ReferenceLookupPort.PractitionerDetails suspendedPractitioner() {
        return new ReferenceLookupPort.PractitionerDetails(
                DOC_TEST_001_ID, DOC_TEST_001_CODE, "Dr. Test Suspended",
                "SUSPENDED", false, true,
                DEPT_TEST_001_ID, "Khoa Kham Benh"
        );
    }
}
