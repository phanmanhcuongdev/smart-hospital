package vn.edu.ptit.smarthms.common.security;

import java.util.Set;

/** Authenticated caller as seen by the module services. */
/** Roles use the plain role names, for example DOCTOR or HR_SPECIALIST. */
public record Actor(String principalName, Set<String> roles) {

    public boolean hasRole(String role) {
        return roles != null && (roles.contains(role) || roles.contains("ADMIN"));
    }

    public boolean hasAnyRole(String... wanted) {
        if (roles == null) {
            return false;
        }
        if (roles.contains("ADMIN")) {
            return true;
        }
        for (String role : wanted) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public void requireRole(String role) {
        if (hasRole(role) == false) {
            throw new vn.edu.ptit.smarthms.common.error.ForbiddenException(
                    "Caller does not have the required role: " + role);
        }
    }

    public void requireAnyRole(String... wanted) {
        if (hasAnyRole(wanted) == false) {
            throw new vn.edu.ptit.smarthms.common.error.ForbiddenException(
                    "Caller does not have any of the required roles.");
        }
    }

    public static Actor anonymous() {
        return new Actor("anonymous", Set.of());
    }
}
