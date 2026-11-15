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
 * Mô tả: Dịch vụ tải thông tin người dùng từ CSDL (sys_user_account) kèm các vai trò và quyền hạn (permissions)
 *        tương ứng, hỗ trợ tra cứu theo username hoặc Bearer token
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
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

    /**
     * Mô tả: Tải thông tin người dùng theo tên đăng nhập (username) phục vụ xác thực khi login
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
     */
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

    /**
     * Mô tả: Tải và xác thực người dùng từ Bearer token cho các request tiếp theo
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
     */
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

    /**
     * Mô tả: Xây dựng danh sách quyền (Authorities) gồm Role và Permissions từ CSDL thông qua RoleHierarchy
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
     */
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
