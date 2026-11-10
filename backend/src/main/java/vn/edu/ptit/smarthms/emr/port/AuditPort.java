package vn.edu.ptit.smarthms.emr.port;

import java.util.UUID;

/** IAuditSink của SYS (IV7 M-19). Tạm thời do EMR cài đặt; V.2 thay bằng bản chuẩn. */
public interface AuditPort {
    /** Ghi một sự kiện thành công cùng transaction. Ném 503 nếu không ghi được (fail closed). */
    void success(String action, String targetType, UUID targetId, UUID actorUserId);

    /** Ghi một thao tác bị từ chối trong transaction riêng, nên vẫn còn dù thao tác gốc bị rollback. */
    void denied(String action, String targetType, UUID targetId);
}