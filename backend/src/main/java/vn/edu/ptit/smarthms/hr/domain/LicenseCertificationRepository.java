package vn.edu.ptit.smarthms.hr.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseCertificationRepository
        extends JpaRepository<LicenseCertification, UUID> {
    List<LicenseCertification> findByEmployeeId(UUID employeeId);
    List<LicenseCertification> findByEmployeeIdAndCredentialType(UUID employeeId,
            String credentialType);
}
