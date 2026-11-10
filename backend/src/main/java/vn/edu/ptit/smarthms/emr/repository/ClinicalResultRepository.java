package vn.edu.ptit.smarthms.emr.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** emr.clinical_result bằng SQL; chống trùng bằng UNIQUE + ON CONFLICT (IV7 M-08). */
@Repository
public class ClinicalResultRepository {
    public record ResultRow(UUID resultId, UUID orderId, String sourceSystem, String sourceResultId,
                            String sourceVersion, OffsetDateTime receivedAt, OffsetDateTime resultAt,
                            String payload, Boolean abnormalFlag, String quality) {}

    private static final String COLUMNS = """
            result_id, order_id, source_system, source_result_id, source_version,
            received_at, result_at, result_payload::text as payload, abnormal_flag, quality
            """;

    private final JdbcTemplate jdbc;

    public ClinicalResultRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** Gọi ngoài transaction để lỗi cú pháp JSON không làm hỏng transaction khác. */
    public boolean isValidJson(String text) {
        try {
            jdbc.queryForObject("select (?::jsonb) is not null", Boolean.class, text);
            return true;
        } catch (DataAccessException e) {
            Throwable root = e.getMostSpecificCause();
            if (root instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("22")) {
                return false;
            }
            throw e;
        }
    }

    public boolean insertIfAbsent(UUID resultId, UUID orderId, String sourceSystem, String sourceResultId,
                                  String sourceVersion, OffsetDateTime resultAt, String payload,
                                  Boolean abnormalFlag, String quality, String actor) {
        return jdbc.update("""
                insert into emr.clinical_result
                  (result_id, order_id, source_system, source_result_id, source_version,
                   received_at, result_at, result_payload, abnormal_flag, quality, created_by, updated_by)
                values (?, ?, ?, ?, ?, now(), ?, ?::jsonb, ?, ?, ?, ?)
                on conflict (source_system, source_result_id, source_version) do nothing
                """, resultId, orderId, sourceSystem, sourceResultId, sourceVersion, resultAt, payload,
                abnormalFlag, quality, actor, actor) == 1;
    }

    public Optional<ResultRow> findById(UUID resultId) {
        return jdbc.query("select " + COLUMNS + " from emr.clinical_result where result_id = ?",
                (rs, n) -> map(rs), resultId).stream().findFirst();
    }

    public Optional<ResultRow> findBySource(String sourceSystem, String sourceResultId, String sourceVersion) {
        return jdbc.query("select " + COLUMNS + """
                 from emr.clinical_result
                where source_system = ? and source_result_id = ? and source_version = ?
                """, (rs, n) -> map(rs), sourceSystem, sourceResultId, sourceVersion).stream().findFirst();
    }

    public List<ResultRow> listByOrder(UUID orderId) {
        return jdbc.query("select " + COLUMNS + " from emr.clinical_result where order_id = ? order by received_at",
                (rs, n) -> map(rs), orderId);
    }

    private static ResultRow map(ResultSet rs) throws SQLException {
        return new ResultRow(rs.getObject("result_id", UUID.class), rs.getObject("order_id", UUID.class),
                rs.getString("source_system"), rs.getString("source_result_id"), rs.getString("source_version"),
                rs.getObject("received_at", OffsetDateTime.class), rs.getObject("result_at", OffsetDateTime.class),
                rs.getString("payload"), rs.getObject("abnormal_flag", Boolean.class), rs.getString("quality"));
    }
}