package vn.edu.ptit.smarthms.system.api;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.edu.ptit.smarthms.common.audit.AuditService;
import vn.edu.ptit.smarthms.common.error.AppException;
import vn.edu.ptit.smarthms.common.error.ErrorCode;
import vn.edu.ptit.smarthms.common.security.SmartHmsUserDetails;
import vn.edu.ptit.smarthms.common.security.TokenService;
import vn.edu.ptit.smarthms.common.util.SecurityUtils;
import vn.edu.ptit.smarthms.system.domain.Session;
import vn.edu.ptit.smarthms.system.domain.UserAccount;
import vn.edu.ptit.smarthms.system.repository.SessionRepository;
import vn.edu.ptit.smarthms.system.repository.UserAccountRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserAccountRepository userAccountRepository;
    private final SessionRepository sessionRepository;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager,
                          TokenService tokenService,
                          UserAccountRepository userAccountRepository,
                          SessionRepository sessionRepository,
                          AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.userAccountRepository = userAccountRepository;
        this.sessionRepository = sessionRepository;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (org.springframework.security.core.AuthenticationException | AppException ex) {
            log.warn("Login failed for user={}", request.username());
            auditService.failure(null, request.username(), "SYS_USER", request.username(), "LOGIN",
                    Map.of("reason", "AUTH_CREDENTIALS_INVALID"));
            throw new AppException(ErrorCode.AUTH_CREDENTIALS_INVALID);
        }

        UserAccount userAccount = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_CREDENTIALS_INVALID));

        String rawToken = tokenService.createSession(userAccount);

        // Retrieve the expiresAt from the newly created session
        String digest = tokenService.hashToken(rawToken);
        Instant expiresAt = sessionRepository.findByTokenDigest(digest)
                .map(Session::getExpiresAt)
                .orElse(Instant.now().plusSeconds(28800));

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .collect(Collectors.toList());

        auditService.success(userAccount.getUserId(), userAccount.getUsername(),
                "SYS_USER", userAccount.getUserId().toString(), "LOGIN", null, null);

        log.info("Login successful for user={}", request.username());
        return ResponseEntity.ok(new LoginResponse(rawToken, expiresAt, userAccount.getUsername(), roles));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        String rawToken = authHeader.substring("Bearer ".length()).trim();
        tokenService.revokeSession(rawToken);

        String username = SecurityUtils.getCurrentUsername();
        log.info("Logout for user={}", username);
        auditService.success(SecurityUtils.getCurrentUserId(), username,
                "SYS_USER", null, "LOGOUT", null, null);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .collect(Collectors.toList());

        List<String> permissions = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.contains(":"))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new MeResponse(username, roles, permissions));
    }
}
