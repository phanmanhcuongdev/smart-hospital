package vn.edu.ptit.smarthms.emr.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;

/** Tạm thời đọc fixture; V.5 thay bằng port thật của HR, SYS và Patient. */
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
                select employee_id, employee_code, full_name
                from hr.employee
                order by full_name
                limit 100
                """, (rs, n) -> new PractitionerSummary(rs.getObject("employee_id", UUID.class),
                        rs.getString("employee_code"), rs.getString("full_name")));
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