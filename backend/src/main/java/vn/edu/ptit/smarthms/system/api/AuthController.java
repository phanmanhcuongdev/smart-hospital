package vn.edu.ptit.smarthms.system.api;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.smarthms.common.error.UnauthorizedException;
import vn.edu.ptit.smarthms.common.security.SmartHmsUserDetails;
import vn.edu.ptit.smarthms.common.security.TokenService;
import vn.edu.ptit.smarthms.system.domain.AuditEvent;
import vn.edu.ptit.smarthms.system.domain.Session;
import vn.edu.ptit.smarthms.system.domain.UserAccount;
import vn.edu.ptit.smarthms.system.repository.SessionRepository;
import vn.edu.ptit.smarthms.system.repository.UserAccountRepository;
import vn.edu.ptit.smarthms.system.service.AuditService;

/** Login / logout / current user (merged from the phung-nam branch). */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String BEARER_PREFIX = "Bearer ";

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
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (DisabledException ex) {
            log.warn("Login rejected for disabled user={}", request.username());
            audit(null, request.username(), AuditEvent.Outcome.DENIED, "LOGIN", "{\"reason\":\"ACCOUNT_DISABLED\"}");
            throw new UnauthorizedException("AUTH_ACCOUNT_DISABLED", "This account is disabled.");
        } catch (BadCredentialsException ex) {
            log.warn("Login failed for user={}", request.username());
            audit(null, request.username(), AuditEvent.Outcome.FAILURE, "LOGIN", "{\"reason\":\"BAD_CREDENTIALS\"}");
            throw new UnauthorizedException("AUTH_CREDENTIALS_INVALID", "Wrong username or password.");
        } catch (AuthenticationException ex) {
            log.warn("Login failed for user={}: {}", request.username(), ex.getMessage());
            audit(null, request.username(), AuditEvent.Outcome.FAILURE, "LOGIN", "{\"reason\":\"AUTH_ERROR\"}");
            throw new UnauthorizedException("AUTH_CREDENTIALS_INVALID", "Wrong username or password.");
        }

        UserAccount userAccount = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnauthorizedException("AUTH_CREDENTIALS_INVALID", "Wrong username or password."));

        String rawToken = tokenService.createSession(userAccount);
        Instant expiresAt = sessionRepository.findByTokenDigest(tokenService.hashToken(rawToken))
                .map(Session::getExpiresAt)
                .orElse(Instant.now().plusSeconds(28800));

        List<String> roles = roles(authentication);
        audit(userAccount.getUserId(), userAccount.getUsername(), AuditEvent.Outcome.SUCCESS, "LOGIN", null);
        log.info("Login successful for user={}", userAccount.getUsername());
        return ResponseEntity.ok(new LoginResponse(rawToken, expiresAt, userAccount.getUsername(), roles));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("AUTH_TOKEN_INVALID", "Missing bearer token.");
        }
        tokenService.revokeSession(authHeader.substring(BEARER_PREFIX.length()).trim());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "unknown";
        log.info("Logout for user={}", username);
        audit(userId(auth), username, AuditEvent.Outcome.SUCCESS, "LOGOUT", null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("AUTH_TOKEN_INVALID", "Not authenticated.");
        }
        List<String> permissions = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.contains(":"))
                .toList();
        return ResponseEntity.ok(new MeResponse(auth.getName(), roles(auth), permissions));
    }

    private static List<String> roles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();
    }

    private static UUID userId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof SmartHmsUserDetails details) {
            return details.getUserId();
        }
        if (authentication.getDetails() instanceof UUID uuid) {
            return uuid;
        }
        return null;
    }

    private void audit(UUID userId, String username, AuditEvent.Outcome outcome, String action, String metadata) {
        try {
            String ref = username == null || username.isBlank() ? "unknown" : username;
            if (ref.length() > 80) {
                ref = ref.substring(0, 80);
            }
            auditService.record(userId, ref, "SYS_USER", userId != null ? userId.toString() : ref,
                    action, outcome, null, metadata);
        } catch (RuntimeException ex) {
            // Auditing must never block sign-in / sign-out.
            log.warn("Could not write {} audit event for user={}: {}", action, username, ex.getMessage());
        }
    }
}
