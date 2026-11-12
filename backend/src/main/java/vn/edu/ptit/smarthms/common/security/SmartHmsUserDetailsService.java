package vn.edu.ptit.smarthms.common.security;

import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.system.domain.Permission;
import vn.edu.ptit.smarthms.system.domain.Role;
import vn.edu.ptit.smarthms.system.domain.RoleAssignment;
import vn.edu.ptit.smarthms.system.domain.Session;
import vn.edu.ptit.smarthms.system.domain.UserAccount;
import vn.edu.ptit.smarthms.system.repository.RoleAssignmentRepository;
import vn.edu.ptit.smarthms.system.repository.UserAccountRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Loads SYS user accounts (sys.user_account) with their active role assignments.
 * Authorities contain the role codes (for example ROLE_DOCTOR) and the permission
 * strings RESOURCE:ACTION:SCOPE. The module services read the roles through
 * {@link ActorResolver}, which strips the ROLE_ prefix.
 */
@Service
public class SmartHmsUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final TokenService tokenService;
    private final RoleHierarchy roleHierarchy;

    public SmartHmsUserDetailsService(UserAccountRepository userAccountRepository,
                                      RoleAssignmentRepository roleAssignmentRepository,
                                      TokenService tokenService,
                                      RoleHierarchy roleHierarchy) {
        this.userAccountRepository = userAccountRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.tokenService = tokenService;
        this.roleHierarchy = roleHierarchy;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!"ACTIVE".equals(account.getAccountStatus())) {
            throw new DisabledException("Account is not active");
        }

        return new SmartHmsUserDetails(account.getUsername(), account.getPasswordHash(),
                account.getUserId(), buildAuthorities(account));
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByToken(String rawToken) {
        Session session = tokenService.validateToken(rawToken)
                .orElseThrow(() -> new BadCredentialsException("Token is invalid, expired or revoked"));
        UserAccount account = session.getUser();

        if (!"ACTIVE".equals(account.getAccountStatus())) {
            throw new DisabledException("Account is not active");
        }

        return new SmartHmsUserDetails(account.getUsername(), account.getPasswordHash(),
                account.getUserId(), buildAuthorities(account));
    }

    private List<GrantedAuthority> buildAuthorities(UserAccount account) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<RoleAssignment> activeAssignments =
                roleAssignmentRepository.findActiveByUserId(account.getUserId(), Instant.now());

        for (RoleAssignment assignment : activeAssignments) {
            Role role = assignment.getRole();
            authorities.add(new SimpleGrantedAuthority(role.getRoleCode()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.toAuthorityString()));
            }
        }
        Collection<? extends GrantedAuthority> reachable =
                roleHierarchy.getReachableGrantedAuthorities(authorities);
        return new ArrayList<>(reachable);
    }
}
