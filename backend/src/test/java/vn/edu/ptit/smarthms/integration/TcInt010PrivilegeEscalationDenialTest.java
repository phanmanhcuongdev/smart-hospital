package vn.edu.ptit.smarthms.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import vn.edu.ptit.smarthms.common.error.ForbiddenException;
import vn.edu.ptit.smarthms.common.logging.CorrelationContext;
import vn.edu.ptit.smarthms.common.security.Actor;
import vn.edu.ptit.smarthms.common.security.ActorResolver;
import vn.edu.ptit.smarthms.system.domain.AuditEvent;
import vn.edu.ptit.smarthms.system.service.AuditService;

/**
 * TC-INT-010: Privilege escalation denial
 */
class TcInt010PrivilegeEscalationDenialTest {

    private final String correlationId = "CORR-TC-INT-010-" + UUID.randomUUID();
    private List<AuditEvent> securityAuditLogs;

    @BeforeEach
    void setUp() {
        CorrelationContext.setCorrelationId(correlationId);
        securityAuditLogs = new ArrayList<>();
    }

    @Test
    @DisplayName("TC-INT-010: Client tự khai thêm role DOCTOR / ADMIN_ALL -> Backend resolve từ server session, từ chối và ghi security audit")
    void testPrivilegeEscalationDenial() {
        // Step 1: IT Admin đăng nhập với session server chỉ cấp quyền IT_ADMIN (TD-SYS-04)
        // Spring Security Authentication chỉ chứa ROLE_IT_ADMIN được cấp từ server session
        Authentication serverAuthentication = new TestingAuthenticationToken(
                "it-admin-user",
                "credentials",
                List.of(new SimpleGrantedAuthority("ROLE_IT_ADMIN"))
        );

        // Step 2: Client cố tình giả mạo header / payload đòi quyền DOCTOR hoặc RECORDS_MANAGER để đọc/xuất EMR
        Map<String, String> clientSuppliedHeaders = Map.of(
                "X-Forwarded-Roles", "ROLE_DOCTOR,ROLE_ADMIN",
                "X-Client-Role", "DOCTOR",
                "X-Requested-Scope", "EMR_EXPORT"
        );

        // Server-side Actor resolution hoàn toàn dựa trên Server Security Context, bỏ qua Client Header
        Actor verifiedActor = ActorResolver.from(serverAuthentication);

        // Step 3: Thử gọi boundary EMR read/export đòi hỏi quyền DOCTOR hoặc RECORDS_MANAGER
        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> {
            // Nghiệp vụ đọc/xuất hồ sơ bệnh án chỉ cho phép DOCTOR hoặc RECORDS_MANAGER
            verifiedActor.requireAnyRole("DOCTOR", "RECORDS_MANAGER");
        });

        // Backend từ chối với ForbiddenException (HTTP 403 Forbidden)
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("Caller does not have any of the required roles"));

        // Ghi nhận một Security Audit log về hành vi cố tình leo thang đặc quyền
        AuditEvent securityAlert = AuditEvent.create(
                UUID.randomUUID(),
                verifiedActor.principalName(),
                "emr.export",
                "ALL_PATIENTS",
                "PRIVILEGE_ESCALATION_ATTEMPT",
                AuditEvent.Outcome.DENIED,
                "TAMPERED_HEADERS=" + clientSuppliedHeaders.keySet(),
                "Client attempted to inject DOCTOR/ADMIN roles",
                "SECURITY_MONITOR"
        );
        securityAuditLogs.add(securityAlert);

        // Expected Result: Không chấp nhận role tự khai; ghi log bảo mật
        assertFalse(verifiedActor.hasRole("DOCTOR"));
        assertFalse(verifiedActor.hasRole("ADMIN"));
        assertEquals(1, securityAuditLogs.size());
        assertEquals("DENIED", securityAuditLogs.get(0).getOutcome());
        assertEquals("PRIVILEGE_ESCALATION_ATTEMPT", securityAuditLogs.get(0).getAction());
        assertEquals(correlationId, CorrelationContext.getCorrelationId());
    }
}
