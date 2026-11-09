package vn.edu.ptit.smarthms.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.ptit.smarthms.common.annotation.RequiresPermission;
import vn.edu.ptit.smarthms.common.audit.AuditService;
import vn.edu.ptit.smarthms.common.error.AppException;
import vn.edu.ptit.smarthms.common.error.ErrorCode;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionCheckAspectTest {

    @Mock
    private AuditService auditService;

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private Signature signature;

    private PermissionCheckAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new PermissionCheckAspect(auditService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RequiresPermission permission(String resource, String action, String scope) {
        return new RequiresPermission() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return RequiresPermission.class;
            }
            @Override
            public String resource() { return resource; }
            @Override
            public String action() { return action; }
            @Override
            public String scope() { return scope; }
        };
    }

    @Test
    void checkPermission_matchingAuthority_shouldProceed() throws Throwable {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "doctor1", null, List.of(new SimpleGrantedAuthority("EMR_ENCOUNTER:READ:OWN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(pjp.proceed()).thenReturn("OK");

        Object result = aspect.checkPermission(pjp, permission("EMR_ENCOUNTER", "READ", "OWN"));

        assertThat(result).isEqualTo("OK");
    }

    @Test
    void checkPermission_missingAuthority_shouldThrowAccessDeniedAndRecordAudit() {
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "nurse1", null, List.of(new SimpleGrantedAuthority("EMR_ENCOUNTER:READ:OWN"))
        );
        auth.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("EncounterController.delete()");

        assertThatThrownBy(() -> aspect.checkPermission(pjp, permission("EMR_ENCOUNTER", "DELETE", "ALL")))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED);

        verify(auditService).denied(eq(userId), eq("nurse1"), eq("EMR_ENCOUNTER"), eq(null), eq("DELETE"), any());
    }
}
