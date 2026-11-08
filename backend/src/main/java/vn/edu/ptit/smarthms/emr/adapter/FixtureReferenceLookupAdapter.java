package vn.edu.ptit.smarthms.emr.adapter;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.edu.ptit.smarthms.emr.port.ReferenceLookupPort;

/** Tạm thời đọc fixture; V.5 thay bằng port thật của HR. */
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
}