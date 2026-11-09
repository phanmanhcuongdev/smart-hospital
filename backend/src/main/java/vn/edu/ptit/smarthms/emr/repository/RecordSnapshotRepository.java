package vn.edu.ptit.smarthms.emr.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc/ghi emr.record_version bằng SQL; ảnh chụp hồ sơ được dựng ngay trong PostgreSQL. */
@Repository
public class RecordSnapshotRepository {
    public record VersionRow(int versionNo, String sha256Hex, OffsetDateTime versionCreatedAt, String payload) {}

    private static final String PAYLOAD_SQL = """
            select jsonb_build_object(
              'encounter', (select to_jsonb(e) from emr.medical_encounter e where e.encounter_id = ?),
              'diagnoses', coalesce((select jsonb_agg(to_jsonb(d) order by d.created_at)
                                     from emr.diagnosis d where d.encounter_id = ?), '[]'::jsonb),
              'observations', coalesce((select jsonb_agg(to_jsonb(o) order by o.observed_at)
                                        from emr.clinical_observation o where o.encounter_id = ?), '[]'::jsonb),
              'orders', coalesce((select jsonb_agg(to_jsonb(m) order by m.created_at)
                                  from emr.medical_order m where m.encounter_id = ?), '[]'::jsonb),
              'prescriptions', coalesce((select jsonb_agg(
                                   to_jsonb(p) || jsonb_build_object('items', coalesce(
                                       (select jsonb_agg(to_jsonb(i) order by i.line_no)
                                        from emr.prescription_item i
                                        where i.prescription_id = p.prescription_id), '[]'::jsonb))
                                   order by p.created_at)
                                 from emr.prescription p where p.encounter_id = ?), '[]'::jsonb)
            )::text
            """;

    private final JdbcTemplate jdbc;

    public RecordSnapshotRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public String buildPayload(UUID encounterId) {
        return jdbc.queryForObject(PAYLOAD_SQL, String.class,
                encounterId, encounterId, encounterId, encounterId, encounterId);
    }

    public void insertVersion(UUID recordId, int versionNo, String payload, String actor) {
        jdbc.update("""
                insert into emr.record_version
                  (record_id, version_no, payload, sha256_hex, created_by, updated_by)
                values (?, ?, ?::jsonb, ?, ?, ?)
                """, recordId, versionNo, payload, sha256(payload), actor, actor);
    }

    public List<VersionRow> list(UUID recordId) {
        return jdbc.query("""
                select version_no, sha256_hex, version_created_at, null::text as payload
                from emr.record_version where record_id = ? order by version_no
                """, (rs, n) -> map(rs), recordId);
    }

    public Optional<VersionRow> get(UUID recordId, int versionNo) {
        return jdbc.query("""
                select version_no, sha256_hex, version_created_at, payload::text as payload
                from emr.record_version where record_id = ? and version_no = ?
                """, (rs, n) -> map(rs), recordId, versionNo).stream().findFirst();
    }

    private static VersionRow map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new VersionRow(rs.getInt("version_no"), rs.getString("sha256_hex"),
                rs.getObject("version_created_at", OffsetDateTime.class), rs.getString("payload"));
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}