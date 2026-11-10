package vn.edu.ptit.smarthms.emr.domain;

import java.util.Set;

public enum EncounterStatus {
    WAITING, IN_PROGRESS, PENDING_LAB_RAD, DIAGNOSED, COMPLETED, CANCELLED, NO_SHOW;

    /** Theo IV7-SM-01. NO_SHOW còn phụ thuộc OPEN-06. */
    public Set<EncounterStatus> allowedNext() {
        return switch (this) {
            case WAITING -> Set.of(IN_PROGRESS, CANCELLED, NO_SHOW);
            case IN_PROGRESS -> Set.of(PENDING_LAB_RAD, DIAGNOSED, CANCELLED);
            case PENDING_LAB_RAD -> Set.of(IN_PROGRESS);
            case DIAGNOSED -> Set.of(COMPLETED);
            case COMPLETED, CANCELLED, NO_SHOW -> Set.of();
        };
    }

    public boolean canTransitionTo(EncounterStatus next) {
        return allowedNext().contains(next);
    }
}