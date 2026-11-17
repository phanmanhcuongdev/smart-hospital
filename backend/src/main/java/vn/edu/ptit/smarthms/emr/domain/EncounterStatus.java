/**
 * Mô tả: Liệt kê các trạng thái của lần khám và quy tắc được chuyển từ trạng thái nào sang trạng thái nào (theo sơ đồ IV7-SM-01).
 * Người thực hiện: Trần Duy Hiếu
 * Ngày thực hiện: 08/11/2026 (cập nhật 10/11/2026)
 */
package vn.edu.ptit.smarthms.emr.domain;

import java.util.Set;

public enum EncounterStatus {
    WAITING, IN_PROGRESS, PENDING_LAB_RAD, DIAGNOSED, COMPLETED, CANCELLED, NO_SHOW;

    /** Giả định, cần đối chiếu State Machine trong IV.7. */
    public Set<EncounterStatus> allowedNext() {
        return switch (this) {
            case WAITING -> Set.of(IN_PROGRESS, CANCELLED, NO_SHOW);
            case IN_PROGRESS -> Set.of(PENDING_LAB_RAD, DIAGNOSED, CANCELLED);
            case PENDING_LAB_RAD -> Set.of(IN_PROGRESS, DIAGNOSED);
            case DIAGNOSED -> Set.of(COMPLETED);
            case COMPLETED, CANCELLED, NO_SHOW -> Set.of();
        };
    }

    public boolean canTransitionTo(EncounterStatus next) {
        return allowedNext().contains(next);
    }
}