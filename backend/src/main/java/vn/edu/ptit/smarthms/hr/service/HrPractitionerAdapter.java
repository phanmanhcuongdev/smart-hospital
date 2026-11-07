package vn.edu.ptit.smarthms.hr.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import vn.edu.ptit.smarthms.emr.service.PractitionerPort;
import vn.edu.ptit.smarthms.hr.domain.Employee;
import vn.edu.ptit.smarthms.hr.domain.EmployeeRepository;

/** HR adapter for the EMR practitioner port (BR-015, BR-016). EMR never queries HR tables. */
@Service
public class HrPractitionerAdapter implements PractitionerPort {

    private final EmployeeRepository employees;
    private final LicenseCertificationService certifications;

    public HrPractitionerAdapter(EmployeeRepository employees,
            LicenseCertificationService certifications) {
        this.employees = employees;
        this.certifications = certifications;
    }

    @Override
    public Optional<UUID> employeeIdForCode(String employeeCode) {
        return employees.findByEmployeeCode(employeeCode).map(Employee::getId);
    }

    @Override
    public boolean isActiveEmployee(UUID employeeId) {
        return employees.findById(employeeId)
                .map(employee -> employee.getStatus() == Employee.Status.ACTIVE)
                .orElse(false);
    }

    @Override
    public boolean hasValidCchn(UUID employeeId, LocalDate today) {
        return certifications.hasValidCchn(employeeId, today);
    }

    @Override
    public String cchnValidity(UUID employeeId, LocalDate today) {
        return certifications.cchnValidityFor(employeeId, today);
    }
}
