package vn.edu.ptit.smarthms.emr.adapter;

import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.ptit.smarthms.emr.port.AuditPort;

/** Ghi sys.audit_event; không ghi nội dung lâm sàng. */
@Component
class JdbcAuditAdapter implements AuditPort {
    private static final String ACTOR_REF = "dev-stub"; // thay bằng người dùng đăng nhập khi V.2 xong

    private final JdbcTemplate jdbc;
    private final TransactionTemplate isolated;

    JdbcAuditAdapter(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.isolated = new TransactionTemplate(transactionManager);
        this.isolated.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void success(String action, String targetType, UUID targetId, UUID actorUserId) {
        try {
            insert(action, targetType, targetId, actorUserId, "SUCCESS");
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Audit unavailable; the operation was not performed");
        }
    }

    @Override
    public void denied(String action, String targetType, UUID targetId) {
        try {
            isolated.executeWithoutResult(status -> insert(action, targetType, targetId, null, "DENIED"));
        } catch (RuntimeException ignored) {
            // Việc từ chối vẫn được thực hiện dù không ghi được dấu vết.
        }
    }

    private void insert(String action, String targetType, UUID targetId, UUID actorUserId, String outcome) {
        jdbc.update("""
                insert into sys.audit_event
                  (audit_id, actor_user_id, actor_ref, target_type, target_id, action, outcome,
                   created_by, updated_by)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), actorUserId, ACTOR_REF, targetType, targetId.toString(), action,
                outcome, ACTOR_REF, ACTOR_REF);
    }
}