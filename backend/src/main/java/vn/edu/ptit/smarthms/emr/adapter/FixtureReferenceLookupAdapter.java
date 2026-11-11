package vn.edu.ptit.smarthms.emr.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;

/** Tích hợp dữ liệu danh mục EMR với HR, SYS và Patient qua JDBC. */
@Component
class FixtureReferenceLookupAdapter implements ReferenceLookupPort {
    private final JdbcTemplate jdbc;

    FixtureReferenceLookupAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean patientExists(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from ref.patient_fixture where patient_id = ?)", Boolean.class, id));
    }

    @Override
    public boolean practitionerExists(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from hr.employee where employee_id = ?)", Boolean.class, id));
    }

    @Override
    public Optional<PractitionerDetails> getPractitionerDetails(UUID id) {
        String sql = """
                select e.employee_id, e.employee_code, e.full_name, e.status,
                       d.department_id, d.display_name as department_name,
                       exists(
                           select 1 from hr.license_certification lc
                           where lc.employee_id = e.employee_id
                             and lc.verification_status = 'VERIFIED'
                             and (lc.valid_from is null or lc.valid_from <= current_date)
                             and (lc.valid_until is null or lc.valid_until >= current_date)
                       ) as has_verified_license
                from hr.employee e
                left join hr.employment emp on emp.employee_id = e.employee_id and emp.end_at is null
                left join hr.department d on emp.department_id = d.department_id
                where e.employee_id = ?
                order by emp.start_at desc nulls last
                limit 1
                """;
        List<PractitionerDetails> list = jdbc.query(sql, (rs, n) -> {
            String status = rs.getString("status");
            boolean active = "ACTIVE".equalsIgnoreCase(status);
            boolean hasLicense = rs.getBoolean("has_verified_license");
            return new PractitionerDetails(
                    rs.getObject("employee_id", UUID.class),
                    rs.getString("employee_code"),
                    rs.getString("full_name"),
                    status,
                    active,
                    hasLicense,
                    rs.getObject("department_id", UUID.class),
                    rs.getString("department_name")
            );
        }, id);
        return list.stream().findFirst();
    }

    @Override
    public boolean departmentExists(UUID id) {
        if (id == null) return false;
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from hr.department where department_id = ?)", Boolean.class, id));
    }

    @Override
    public boolean isDepartmentActive(UUID id) {
        if (id == null) return false;
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from hr.department where department_id = ? and active)", Boolean.class, id));
    }

    @Override
    public List<DepartmentSummary> listDepartments() {
        return jdbc.query("""
                select department_id, department_code, display_name, department_type, active
                from hr.department
                where active
                order by display_name
                limit 100
                """, (rs, n) -> new DepartmentSummary(
                rs.getObject("department_id", UUID.class),
                rs.getString("department_code"),
                rs.getString("display_name"),
                rs.getString("department_type"),
                rs.getBoolean("active")
        ));
    }

    @Override
    public Optional<String> activeMedicationLabel(UUID id) {
        return jdbc.query(
                "select display_name from ref.medication_fixture where medication_id = ? and is_active",
                (rs, n) -> rs.getString(1), id).stream().findFirst();
    }

    @Override
    public boolean userExists(UUID id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from sys.user_account where user_id = ?)", Boolean.class, id));
    }

    @Override
    public List<PatientSummary> searchPatients(String query) {
        String escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return jdbc.query("""
                select patient_id, display_name, identity_status
                from ref.patient_fixture
                where lower(display_name) like lower(?) escape '\\'
                order by display_name
                limit 50
                """, (rs, n) -> new PatientSummary(rs.getObject("patient_id", UUID.class),
                        rs.getString("display_name"), rs.getString("identity_status")),
                "%" + escaped + "%");
    }

    @Override
    public List<PractitionerSummary> listPractitioners() {
        return jdbc.query("""
                select e.employee_id, e.employee_code, e.full_name, e.status,
                       d.display_name as department_name,
                       exists(
                           select 1 from hr.license_certification lc
                           where lc.employee_id = e.employee_id
                             and lc.verification_status = 'VERIFIED'
                             and (lc.valid_from is null or lc.valid_from <= current_date)
                             and (lc.valid_until is null or lc.valid_until >= current_date)
                       ) as has_verified_license
                from hr.employee e
                left join hr.employment emp on emp.employee_id = e.employee_id and emp.end_at is null
                left join hr.department d on emp.department_id = d.department_id
                order by e.full_name
                limit 100
                """, (rs, n) -> new PractitionerSummary(
                rs.getObject("employee_id", UUID.class),
                rs.getString("employee_code"),
                rs.getString("full_name"),
                rs.getString("status"),
                "ACTIVE".equalsIgnoreCase(rs.getString("status")),
                rs.getString("department_name"),
                rs.getBoolean("has_verified_license")
        ));
    }

    @Override
    public List<MedicationSummary> listMedications() {
        return jdbc.query("""
                select medication_id, display_name
                from ref.medication_fixture
                where is_active
                order by display_name
                limit 200
                """, (rs, n) -> new MedicationSummary(rs.getObject("medication_id", UUID.class),
                        rs.getString("display_name")));
    }

    @Override
    public List<UserSummary> listUsers() {
        return jdbc.query("""
                select user_id, username
                from sys.user_account
                where account_status = 'ACTIVE'
                order by username
                limit 100
                """, (rs, n) -> new UserSummary(rs.getObject("user_id", UUID.class),
                        rs.getString("username")));
    }
}
