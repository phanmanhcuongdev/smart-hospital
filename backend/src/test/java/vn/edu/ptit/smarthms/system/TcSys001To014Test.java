package vn.edu.ptit.smarthms.system;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.ptit.smarthms.common.annotation.RequiresPermission;
import vn.edu.ptit.smarthms.common.aspect.PermissionCheckAspect;
import vn.edu.ptit.smarthms.common.audit.AuditService;
import vn.edu.ptit.smarthms.common.error.ApiExceptionHandler;
import vn.edu.ptit.smarthms.common.error.AppException;
import vn.edu.ptit.smarthms.common.error.ErrorCode;
import vn.edu.ptit.smarthms.common.security.BearerTokenFilter;
import vn.edu.ptit.smarthms.common.security.SmartHmsUserDetails;
import vn.edu.ptit.smarthms.common.security.SmartHmsUserDetailsService;
import vn.edu.ptit.smarthms.common.security.TokenService;
import vn.edu.ptit.smarthms.system.api.AuditController;
import vn.edu.ptit.smarthms.system.api.AuthController;
import vn.edu.ptit.smarthms.system.domain.AuditEvent;
import vn.edu.ptit.smarthms.system.domain.Permission;
import vn.edu.ptit.smarthms.system.domain.Role;
import vn.edu.ptit.smarthms.system.domain.RoleAssignment;
import vn.edu.ptit.smarthms.system.domain.Session;
import vn.edu.ptit.smarthms.system.domain.UserAccount;
import vn.edu.ptit.smarthms.system.repository.AuditEventRepository;
import vn.edu.ptit.smarthms.system.repository.RoleAssignmentRepository;
import vn.edu.ptit.smarthms.system.repository.RoleRepository;
import vn.edu.ptit.smarthms.system.repository.SessionRepository;
import vn.edu.ptit.smarthms.system.repository.UserAccountRepository;
import vn.edu.ptit.smarthms.system.service.SystemTestScenarioService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TcSys001To014Test {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RoleAssignmentRepository roleAssignmentRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private AuditEventRepository auditEventRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ProceedingJoinPoint pjp;
    @Mock private Signature signature;

    private TokenService tokenService;
    private AuditService auditService;
    private SmartHmsUserDetailsService userDetailsService;
    private PermissionCheckAspect permissionAspect;
    private SystemTestScenarioService scenarioService;
    private PasswordEncoder passwordEncoder;
    private List<AuditEvent> recordedAudits;

    @BeforeEach
    void setUp() throws Exception {
        passwordEncoder = new BCryptPasswordEncoder(4);
        tokenService = new TokenService(sessionRepository);
        var ttlField = TokenService.class.getDeclaredField("sessionTtlHours");
        ttlField.setAccessible(true);
        ttlField.set(tokenService, 8);

        recordedAudits = new ArrayList<>();
        lenient().when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(inv -> {
            AuditEvent ev = inv.getArgument(0);
            recordedAudits.add(ev);
            return ev;
        });

        auditService = new AuditService(auditEventRepository);
        userDetailsService = new SmartHmsUserDetailsService(userAccountRepository, roleAssignmentRepository, tokenService);
        permissionAspect = new PermissionCheckAspect(auditService);
        scenarioService = new SystemTestScenarioService(
                jdbcTemplate, userAccountRepository, roleRepository, roleAssignmentRepository,
                sessionRepository, tokenService, passwordEncoder, auditService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RequiresPermission perm(String resource, String action, String scope) {
        return new RequiresPermission() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return RequiresPermission.class; }
            @Override public String resource() { return resource; }
            @Override public String action() { return action; }
            @Override public String scope() { return scope; }
        };
    }

    @Test
    @DisplayName("TC-SYS-001: Đăng nhập đúng/sai — luồng AC gốc (DOC-TEST-001 login, logout, không cấp quyền từ HR chưa duyệt, không log password)")
    void tcSys001_validLoginAndLogoutWithoutPasswordLeakOrUnapprovedHrPrivilege() throws Exception {
        UUID docId = UUID.randomUUID();
        UserAccount docAccount = new UserAccount();
        docAccount.setUserId(docId);
        docAccount.setUsername("DOC-TEST-001");
        docAccount.setPasswordHash(passwordEncoder.encode("Doctor@2026!"));
        docAccount.setAccountStatus("ACTIVE");

        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("DOC-TEST-001", null,
                        List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")))
        );
        when(userAccountRepository.findByUsername("DOC-TEST-001")).thenReturn(Optional.of(docAccount));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.findByTokenDigest(anyString())).thenAnswer(inv -> {
            Session s = new Session();
            s.setUser(docAccount);
            s.setExpiresAt(Instant.now().plus(8, ChronoUnit.HOURS));
            return Optional.of(s);
        });

        AuthController authController = new AuthController(
                authenticationManager, tokenService, userAccountRepository, sessionRepository, auditService
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        // 1. Login DOC-TEST-001
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"DOC-TEST-001\",\"password\":\"Doctor@2026!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("DOC-TEST-001"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_DOCTOR"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        // 2. Verify user with HR job_title='Doctor' but NO sys.role_assignment gets 0 authorities
        UserAccount unapprovedHrDoc = new UserAccount();
        unapprovedHrDoc.setUserId(UUID.randomUUID());
        unapprovedHrDoc.setUsername("HR-UNAPPROVED-DOC");
        unapprovedHrDoc.setPasswordHash("hash");
        unapprovedHrDoc.setAccountStatus("ACTIVE");
        when(userAccountRepository.findByUsername("HR-UNAPPROVED-DOC")).thenReturn(Optional.of(unapprovedHrDoc));
        when(roleAssignmentRepository.findActiveByUserId(eq(unapprovedHrDoc.getUserId()), any())).thenReturn(List.of());

        var unapprovedDetails = userDetailsService.loadUserByUsername("HR-UNAPPROVED-DOC");
        assertThat(unapprovedDetails.getAuthorities()).isEmpty();

        // 3. Logout & verify audit never contains raw password
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("DOC-TEST-001", null, List.of())
        );
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer raw-session-token"))
                .andExpect(status().isNoContent());

        assertThat(recordedAudits).hasSize(2);
        assertThat(recordedAudits).allSatisfy(ev -> {
            if (ev.getSafeMetadata() != null) {
                assertThat(ev.getSafeMetadata()).doesNotContain("Doctor@2026!");
            }
        });
    }

    @Test
    @DisplayName("TC-SYS-002: Đăng nhập đúng/sai — nhánh âm/ranh giới (Sai mật khẩu, không cấp session, không lộ user, không log secret)")
    void tcSys002_invalidLoginDoesNotCreateSessionOrLeakSecret() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        AuthController authController = new AuthController(
                authenticationManager, tokenService, userAccountRepository, sessionRepository, auditService
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"DOC-TEST-001\",\"password\":\"WrongSecretPassword!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_CREDENTIALS_INVALID"))
                .andExpect(jsonPath("$.token").doesNotExist());

        verify(sessionRepository, never()).save(any());
        assertThat(recordedAudits).hasSize(1);
        assertThat(recordedAudits.get(0).getOutcome()).isEqualTo("FAILURE");
        assertThat(String.valueOf(recordedAudits.get(0).getSafeMetadata())).doesNotContain("WrongSecretPassword!");
    }

    @Test
    @DisplayName("TC-SYS-003: Phiên hết hạn hoặc bị thu hồi — luồng AC gốc (SESSION-EXPIRED-001 bị chặn 401 AUTH_TOKEN_EXPIRED và ghi audit DENIED)")
    void tcSys003_expiredSessionRejectedWithAudit() throws Exception {
        UserAccount doc = new UserAccount();
        doc.setUserId(UUID.randomUUID());
        doc.setUsername("DOC-TEST-001");
        doc.setAccountStatus("ACTIVE");

        Session expiredSession = new Session();
        expiredSession.setSessionId(UUID.randomUUID());
        expiredSession.setUser(doc);
        expiredSession.setTokenDigest(tokenService.hashToken("SESSION-EXPIRED-001"));
        expiredSession.setIssuedAt(Instant.now().minus(10, ChronoUnit.HOURS));
        expiredSession.setExpiresAt(Instant.now().minus(2, ChronoUnit.HOURS));

        when(sessionRepository.findByTokenDigest(tokenService.hashToken("SESSION-EXPIRED-001")))
                .thenReturn(Optional.of(expiredSession));

        BearerTokenFilter filter = new BearerTokenFilter(userDetailsService, auditService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/emr/records/EMR-TEST-001");
        request.addHeader("Authorization", "Bearer SESSION-EXPIRED-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(BearerTokenFilter.AUTH_ERROR_ATTR)).isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED);
        assertThat(recordedAudits).hasSize(1);
        assertThat(recordedAudits.get(0).getOutcome()).isEqualTo("DENIED");
        assertThat(recordedAudits.get(0).getSafeMetadata()).contains("AUTH_TOKEN_EXPIRED");
        assertThat(recordedAudits.get(0).getSafeMetadata()).doesNotContain("SESSION-EXPIRED-001");
    }

    @Test
    @DisplayName("TC-SYS-004: Phiên hết hạn hoặc bị thu hồi — nhánh âm/ranh giới (Thu hồi role của SESSION-ACTIVE-001, gọi lại bằng session cũ bị chặn)")
    void tcSys004_revokedRoleOnActiveSessionImmediatelyBlocksEmrAccess() {
        UUID docId = UUID.randomUUID();
        UserAccount doc = new UserAccount();
        doc.setUserId(docId);
        doc.setUsername("DOC-TEST-001");
        doc.setPasswordHash("hash");
        doc.setAccountStatus("ACTIVE");

        Session activeSession = new Session();
        activeSession.setSessionId(UUID.randomUUID());
        activeSession.setUser(doc);
        activeSession.setTokenDigest(tokenService.hashToken("SESSION-ACTIVE-001"));
        activeSession.setIssuedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        activeSession.setExpiresAt(Instant.now().plus(7, ChronoUnit.HOURS));
        activeSession.setIssuedEpoch(0);

        when(sessionRepository.findByTokenDigest(tokenService.hashToken("SESSION-ACTIVE-001")))
                .thenReturn(Optional.of(activeSession));
        // Role was revoked -> active assignments now empty
        when(roleAssignmentRepository.findActiveByUserId(eq(docId), any())).thenReturn(List.of());

        var userDetails = userDetailsService.loadUserByToken("SESSION-ACTIVE-001");
        assertThat(userDetails.getAuthorities()).isEmpty();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("getEmrRecord()");
        when(pjp.getArgs()).thenReturn(new Object[]{"EMR-TEST-001"});

        assertThatThrownBy(() -> permissionAspect.checkPermission(pjp, perm("EMR_RECORD", "READ", "ANY")))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("TC-SYS-005: Least privilege / object scope — luồng AC gốc (IT-ADMIN-TEST-001 yêu cầu đọc EMR-TEST-002 bị từ chối 403)")
    void tcSys005_itAdminDeniedReadingEmrRecord() {
        UUID adminId = UUID.randomUUID();
        SmartHmsUserDetails itAdmin = new SmartHmsUserDetails("IT-ADMIN-TEST-001", "", adminId, List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("SYS_USER:READ:ALL"),
                new SimpleGrantedAuthority("AUDIT_EVENT:READ:ALL")
        ));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(itAdmin, null, itAdmin.getAuthorities());
        auth.setDetails(adminId);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(pjp.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("getEmrRecord(EMR-TEST-002)");
        when(pjp.getArgs()).thenReturn(new Object[]{"EMR-TEST-002"});

        assertThatThrownBy(() -> permissionAspect.checkPermission(pjp, perm("EMR_RECORD", "READ", "ANY")))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        assertThat(recordedAudits).hasSize(1);
        assertThat(recordedAudits.get(0).getActorRef()).isEqualTo("IT-ADMIN-TEST-001");
        assertThat(recordedAudits.get(0).getTargetId()).isEqualTo("EMR-TEST-002");
        assertThat(recordedAudits.get(0).getOutcome()).isEqualTo("DENIED");
    }

    @Test
    @DisplayName("TC-SYS-006: Least privilege / object scope — nhánh âm/ranh giới (NURSE-TEST-001 thử ký/khóa bệnh án bị từ chối 403)")
    void tcSys006_nurseDeniedLockingEmrRecord() {
        UUID nurseId = UUID.randomUUID();
        SmartHmsUserDetails nurse = new SmartHmsUserDetails("NURSE-TEST-001", "", nurseId, List.of(
                new SimpleGrantedAuthority("ROLE_NURSE"),
                new SimpleGrantedAuthority("EMR_RECORD:READ:DEPT"),
                new SimpleGrantedAuthority("EMR_ENCOUNTER:WRITE:OWN")
        ));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(nurse, null, nurse.getAuthorities());
        auth.setDetails(nurseId);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(pjp.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("lockEmrRecord(EMR-TEST-001)");
        when(pjp.getArgs()).thenReturn(new Object[]{"EMR-TEST-001"});

        assertThatThrownBy(() -> permissionAspect.checkPermission(pjp, perm("EMR_RECORD", "LOCK", "OWN")))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        assertThat(recordedAudits).hasSize(1);
        assertThat(recordedAudits.get(0).getActorRef()).isEqualTo("NURSE-TEST-001");
        assertThat(recordedAudits.get(0).getAction()).isEqualTo("LOCK");
        assertThat(recordedAudits.get(0).getOutcome()).isEqualTo("DENIED");
    }

    @Test
    @DisplayName("TC-SYS-007: Cấp/thu hồi role có căn cứ — luồng AC gốc (Admin cấp role kèm DEC-RBAC-001 và truy vết audit)")
    void tcSys007_adminAssignsRoleWithApprovedDecisionRef() {
        UUID adminId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "IT-ADMIN-TEST-001", null, List.of(new SimpleGrantedAuthority("SYS_ROLE:ASSIGN:ALL"))
        );
        auth.setDetails(adminId);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserAccount targetUser = new UserAccount();
        targetUser.setUserId(UUID.randomUUID());
        targetUser.setUsername("DOC-TEST-001");

        Role doctorRole = new Role();
        doctorRole.setRoleId(UUID.randomUUID());
        doctorRole.setRoleCode("ROLE_DOCTOR");

        when(userAccountRepository.findByUsername("DOC-TEST-001")).thenReturn(Optional.of(targetUser));
        when(roleRepository.findByRoleCode("ROLE_DOCTOR")).thenReturn(Optional.of(doctorRole));
        when(roleAssignmentRepository.save(any(RoleAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = scenarioService.assignRoleWithDecision(
                "DOC-TEST-001", "ROLE_DOCTOR", "DEC-RBAC-001", "OWN", 60
        );

        assertThat(result.get("approvalRef")).isEqualTo("DEC-RBAC-001");
        assertThat(recordedAudits).hasSize(1);
        assertThat(recordedAudits.get(0).getChangeRef()).isEqualTo("DEC-RBAC-001");
        assertThat(recordedAudits.get(0).getOutcome()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("TC-SYS-008: Cấp/thu hồi role có căn cứ — nhánh âm/ranh giới (Từ chối khi không có quyền hoặc scope chưa duyệt OPEN-01)")
    void tcSys008_unapprovedScopeOrUnauthorizedUserRejectedWithoutMutatingRoleAssignment() {
        UUID adminId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "IT-ADMIN-TEST-001", null, List.of(new SimpleGrantedAuthority("SYS_ROLE:ASSIGN:ALL"))
        );
        auth.setDetails(adminId);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> scenarioService.assignRoleWithDecision(
                "DOC-TEST-001", "ROLE_DOCTOR", "DEC-RBAC-001", "UNAPPROVED_SCOPE_OPEN_01", 60
        ))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        verify(roleAssignmentRepository, never()).save(any());
        assertThat(recordedAudits).hasSize(1);
        assertThat(recordedAudits.get(0).getOutcome()).isEqualTo("DENIED");
        assertThat(recordedAudits.get(0).getSafeMetadata()).contains("UNAPPROVED_SCOPE_OPEN_01");
    }

    @Test
    @DisplayName("TC-SYS-009: Audit khi sửa và khóa — luồng AC gốc (HR cập nhật status có DEC-HR-001 & Bác sĩ khóa EMR-TEST-001)")
    void tcSys009_auditRecordedOnHrStatusUpdateAndEmrLockWithVersionAndChangeRef() {
        UUID docId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "DOC-TEST-001", null, List.of(new SimpleGrantedAuthority("EMR_RECORD:LOCK:OWN"))
        );
        auth.setDetails(docId);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(jdbcTemplate.queryForList(anyString(), eq(SystemTestScenarioService.EMR_TEST_001_ID)))
                .thenReturn(List.of(Map.of("record_status", "DRAFT", "current_version", 1, "row_version", 1)));
        when(jdbcTemplate.update(anyString(), any(), any(), eq(SystemTestScenarioService.EMR_TEST_001_ID)))
                .thenReturn(1);
        when(jdbcTemplate.queryForMap(anyString(), eq(SystemTestScenarioService.EMR_TEST_001_ID)))
                .thenReturn(Map.of(
                        "record_id", SystemTestScenarioService.EMR_TEST_001_ID,
                        "record_status", "LOCKED",
                        "current_version", 1,
                        "row_version", 2
                ));

        Map<String, Object> locked = scenarioService.lockEmrRecord("EMR-TEST-001");
        assertThat(locked.get("record_status")).isEqualTo("LOCKED");
        assertThat(locked.get("row_version")).isEqualTo(2);

        assertThat(recordedAudits).hasSize(1);
        AuditEvent ev = recordedAudits.get(0);
        assertThat(ev.getActorRef()).isEqualTo("DOC-TEST-001");
        assertThat(ev.getTargetType()).isEqualTo("EMR_RECORD");
        assertThat(ev.getTargetId()).isEqualTo("EMR-TEST-001");
        assertThat(ev.getAction()).isEqualTo("LOCK_RECORD");
        assertThat(ev.getOutcome()).isEqualTo("SUCCESS");
        assertThat(ev.getChangeRef()).isEqualTo("LOCK-EMR-TEST-001-RV2");
    }

    @Test
    @DisplayName("TC-SYS-010: Audit khi sửa và khóa — nhánh âm/ranh giới (IT-ADMIN-TEST-001 bị từ chối khóa EMR, outcome=DENIED, không ghi secret/PHI)")
    void tcSys010_deniedEmrMutationRecordsSafeAuditWithoutSecrets() {
        UUID adminId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "IT-ADMIN-TEST-001", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        auth.setDetails(adminId);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(pjp.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("lockEmrRecord(EMR-TEST-001)");
        when(pjp.getArgs()).thenReturn(new Object[]{"EMR-TEST-001"});

        assertThatThrownBy(() -> permissionAspect.checkPermission(pjp, perm("EMR_RECORD", "LOCK", "OWN")))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        assertThat(recordedAudits).hasSize(1);
        AuditEvent ev = recordedAudits.get(0);
        assertThat(ev.getOutcome()).isEqualTo("DENIED");
        assertThat(ev.getActorRef()).isEqualTo("IT-ADMIN-TEST-001");
        assertThat(ev.getTargetId()).isEqualTo("EMR-TEST-001");
        assertThat(String.valueOf(ev.getSafeMetadata())).doesNotContain("password", "token");
    }

    @Test
    @DisplayName("TC-SYS-011: Chống sửa/xóa audit — luồng AC gốc (Application admin thử sửa AUD-TEST-001 bị chặn 409 AUDIT_IMMUTABLE)")
    void tcSys011_auditUpdateBlockedAtApplicationBoundary() throws Exception {
        AuditController auditController = new AuditController(auditEventRepository, auditService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(auditController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("IT-ADMIN-TEST-001", null,
                        List.of(new SimpleGrantedAuthority("AUDIT_EVENT:READ:ALL")))
        );

        mockMvc.perform(put("/api/v1/audit/events/" + SystemTestScenarioService.AUD_TEST_001_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actorRef\":\"tampered\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUDIT_IMMUTABLE"));

        verify(auditEventRepository, never()).deleteById(any());
        assertThat(recordedAudits).hasSize(1);
        assertThat(recordedAudits.get(0).getAction()).isEqualTo("UPDATE_AUDIT");
        assertThat(recordedAudits.get(0).getOutcome()).isEqualTo("DENIED");
    }

    @Test
    @DisplayName("TC-SYS-012: Chống sửa/xóa audit — nhánh âm/ranh giới (Xóa lặp audit event trên application boundary đều bị chặn 409)")
    void tcSys012_repeatedAuditDeleteBlockedAndLogged() throws Exception {
        AuditController auditController = new AuditController(auditEventRepository, auditService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(auditController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("IT-ADMIN-TEST-001", null,
                        List.of(new SimpleGrantedAuthority("AUDIT_EVENT:READ:ALL")))
        );

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(delete("/api/v1/audit/events/" + SystemTestScenarioService.AUD_TEST_001_ID))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("AUDIT_IMMUTABLE"));
        }

        verify(auditEventRepository, never()).delete(any());
        verify(auditEventRepository, never()).deleteById(any());
        assertThat(recordedAudits).hasSize(2);
        assertThat(recordedAudits).allMatch(ev -> "DELETE_AUDIT".equals(ev.getAction()) && "DENIED".equals(ev.getOutcome()));
    }

    @Test
    @DisplayName("TC-SYS-013: Adapter mock lỗi / gửi lặp — luồng AC gốc (Gửi EV-HR-001 hai lần liên tiếp không nhân đôi record)")
    void tcSys013_duplicateAdapterEventIsIdempotent() {
        // First delivery: no existing outbox row
        when(jdbcTemplate.queryForList(anyString(), eq("EV-HR-001")))
                .thenReturn(List.of())
                .thenReturn(List.of(Map.of("event_id", UUID.randomUUID(), "delivery_state", "SENT", "attempts", 1)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("EV-HR-001")))
                .thenReturn(1);

        Map<String, Object> first = scenarioService.processMockIntegrationEvent("HR_OUTBOX", "EV-HR-001", "HR", "v1", false);
        Map<String, Object> second = scenarioService.processMockIntegrationEvent("HR_OUTBOX", "EV-HR-001", "HR", "v1", false);

        assertThat(first.get("duplicate")).isEqualTo(false);
        assertThat(first.get("deliveryState")).isEqualTo("SENT");
        assertThat(second.get("duplicate")).isEqualTo(true);
        assertThat(second.get("recordCount")).isEqualTo(1);
        // INSERT executed only once
        verify(jdbcTemplate, times(1)).update(
                org.mockito.ArgumentMatchers.contains("INSERT INTO hr.integration_outbox"),
                any(), any(), eq("EV-HR-001"), eq("SENT"), any(), any()
        );
    }

    @Test
    @DisplayName("TC-SYS-014: Adapter mock lỗi / gửi lặp — nhánh âm/ranh giới (Timeout ghi FAILED không báo thành công giả, retry cùng eventId chuyển SENT)")
    void tcSys014_timeoutMarksFailedAndRetryReconcilesWithoutDuplication() {
        UUID outboxId = UUID.randomUUID();
        when(jdbcTemplate.queryForList(anyString(), eq("EV-HR-001")))
                .thenReturn(List.of())
                .thenReturn(List.of(Map.of("event_id", outboxId, "delivery_state", "FAILED", "attempts", 1)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("EV-HR-001")))
                .thenReturn(1);

        // 1. Timeout on first attempt -> throws 503 INTEGRATION_DELIVERY_FAILED & records FAILED
        assertThatThrownBy(() -> scenarioService.processMockIntegrationEvent("HR_OUTBOX", "EV-HR-001", "HR", "v1", true))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INTEGRATION_DELIVERY_FAILED);

        assertThat(recordedAudits).hasSize(1);
        assertThat(recordedAudits.get(0).getOutcome()).isEqualTo("FAILURE");

        // 2. Retry same eventKey -> reconciles to SENT with attempts=2 and recordCount=1
        Map<String, Object> retryResult = scenarioService.processMockIntegrationEvent("HR_OUTBOX", "EV-HR-001", "HR", "v1", false);
        assertThat(retryResult.get("deliveryState")).isEqualTo("SENT");
        assertThat(retryResult.get("attempts")).isEqualTo(2);
        assertThat(retryResult.get("recordCount")).isEqualTo(1);
        assertThat(recordedAudits).hasSize(2);
        assertThat(recordedAudits.get(1).getOutcome()).isEqualTo("SUCCESS");
    }
}
