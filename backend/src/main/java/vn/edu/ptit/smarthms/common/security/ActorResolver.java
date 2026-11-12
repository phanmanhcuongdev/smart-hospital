package vn.edu.ptit.smarthms.common.security;

import java.util.HashSet;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Builds an Actor from the Spring Security authentication of the current request. */
public final class ActorResolver {

    private ActorResolver() {
    }

    public static Actor from(Authentication authentication) {
        if (authentication == null || authentication.isAuthenticated() == false) {
            return Actor.anonymous();
        }
        Set<String> roles = new HashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.startsWith("ROLE_")) {
                String stripped = role.substring("ROLE_".length());
                roles.add(stripped);
                if ("RECEPTIONIST".equals(stripped)) {
                    roles.add("RECEPTION");
                }
            } else {
                roles.add(role);
            }
        }
        return new Actor(authentication.getName(), Set.copyOf(roles));
    }
}
