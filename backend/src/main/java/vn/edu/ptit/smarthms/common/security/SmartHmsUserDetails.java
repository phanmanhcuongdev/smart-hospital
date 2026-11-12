package vn.edu.ptit.smarthms.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

/**
 * Extended UserDetails that also carries the UUID of the user account.
 */
public class SmartHmsUserDetails extends User {

    private final UUID userId;

    public SmartHmsUserDetails(String username, String password, UUID userId,
                               Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
