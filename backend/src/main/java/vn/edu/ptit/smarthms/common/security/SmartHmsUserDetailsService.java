package vn.edu.ptit.smarthms.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.smarthms.common.error.AppException;
import vn.edu.ptit.smarthms.common.error.ErrorCode;
import vn.edu.ptit.smarthms.system.domain.Permission;
import vn.edu.ptit.smarthms.system.domain.Role;
import vn.edu.ptit.smarthms.system.domain.RoleAssignment;
import vn.edu.ptit.smarthms.system.domain.Session;
import vn.edu.ptit.smarthms.system.domain.UserAccount;
import vn.edu.ptit.smarthms.system.repository.RoleAssignmentRepository;
import vn.edu.ptit.smarthms.system.repository.UserAccountRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SmartHmsUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final TokenService tokenService;

    public SmartHmsUserDetailsService(UserAccountRepository userAccountRepository,
                                      RoleAssignmentRepository roleAssignmentRepository,
                                      TokenService tokenService) {
        this.userAccountRepository = userAccountRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!"ACTIVE".equals(account.getAccountStatus())) {
            throw new AppException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        List<GrantedAuthority> authorities = buildAuthorities(account);

        return User.builder()
                .username(account.getUsername())
                .password(account.getPasswordHash())
                .authorities(authorities)
                .build();
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByToken(String rawToken) {
        Optional<Session> sessionOpt = tokenService.validateToken(rawToken);
        if (sessionOpt.isEmpty()) {
            throw new AppException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        Session session = sessionOpt.get();
        UserAccount account = session.getUser();

        if (!"ACTIVE".equals(account.getAccountStatus())) {
            throw new AppException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        List<GrantedAuthority> authorities = buildAuthorities(account);

        return new SmartHmsUserDetails(account.getUsername(), account.getPasswordHash(),
                account.getUserId(), authorities);
    }

    private List<GrantedAuthority> buildAuthorities(UserAccount account) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        List<RoleAssignment> activeAssignments =
                roleAssignmentRepository.findActiveByUserId(account.getUserId(), Instant.now());

        for (RoleAssignment assignment : activeAssignments) {
            Role role = assignment.getRole();
            // Add role authority
            authorities.add(new SimpleGrantedAuthority(role.getRoleCode()));
            // Add permission authorities
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.toAuthorityString()));
            }
        }
        return authorities;
    }
}
