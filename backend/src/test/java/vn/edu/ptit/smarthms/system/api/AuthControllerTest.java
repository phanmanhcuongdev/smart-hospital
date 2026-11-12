package vn.edu.ptit.smarthms.system.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.ptit.smarthms.common.error.ApiExceptionHandler;
import vn.edu.ptit.smarthms.common.security.SmartHmsUserDetails;
import vn.edu.ptit.smarthms.common.security.TokenService;
import vn.edu.ptit.smarthms.system.domain.Session;
import vn.edu.ptit.smarthms.system.domain.UserAccount;
import vn.edu.ptit.smarthms.system.repository.SessionRepository;
import vn.edu.ptit.smarthms.system.repository.UserAccountRepository;
import vn.edu.ptit.smarthms.system.service.AuditService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                authenticationManager,
                tokenService,
                userAccountRepository,
                sessionRepository,
                auditService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_success_shouldReturn200WithToken() throws Exception {
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount();
        user.setUserId(userId);
        user.setUsername("admin");
        user.setPasswordHash("hash");
        user.setAccountStatus("ACTIVE");

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(userAccountRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(tokenService.createSession(user)).thenReturn("raw-token-value");
        when(tokenService.hashToken("raw-token-value")).thenReturn("hash-digest");

        Session session = new Session();
        session.setExpiresAt(Instant.now().plusSeconds(28800));
        when(sessionRepository.findByTokenDigest("hash-digest")).thenReturn(Optional.of(session));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@2026!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("raw-token-value"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    void login_badCredentials_shouldReturn401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_CREDENTIALS_INVALID"));
    }

    @Test
    void login_missingUsername_shouldReturn400ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"pass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void logout_validBearerToken_shouldReturn204AndRevokeSession() throws Exception {
        UUID userId = UUID.randomUUID();
        SmartHmsUserDetails userDetails = new SmartHmsUserDetails("admin", "", userId,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        auth.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer my-token-123"))
                .andExpect(status().isNoContent());

        verify(tokenService).revokeSession("my-token-123");
    }

    @Test
    void me_authenticated_shouldReturnUserInfo() throws Exception {
        UUID userId = UUID.randomUUID();
        SmartHmsUserDetails userDetails = new SmartHmsUserDetails("admin", "", userId,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("SYS_USER:READ:ALL")));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        auth.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.permissions[0]").value("SYS_USER:READ:ALL"));
    }
}
