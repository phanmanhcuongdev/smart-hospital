package vn.edu.ptit.smarthms.common.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import vn.edu.ptit.smarthms.common.annotation.RequiresPermission;
import vn.edu.ptit.smarthms.common.audit.AuditService;
import vn.edu.ptit.smarthms.common.error.AppException;
import vn.edu.ptit.smarthms.common.error.ErrorCode;
import vn.edu.ptit.smarthms.common.util.SecurityUtils;

import java.util.Map;

@Aspect
@Component
public class PermissionCheckAspect {

    private static final Logger log = LoggerFactory.getLogger(PermissionCheckAspect.class);

    private final AuditService auditService;

    public PermissionCheckAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint pjp, RequiresPermission requiresPermission) throws Throwable {
        String resource = requiresPermission.resource();
        String action = requiresPermission.action();
        String scope = requiresPermission.scope();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        boolean hasPermission = hasMatchingAuthority(auth, resource, action, scope);
        if (!hasPermission) {
            String username = SecurityUtils.getCurrentUsername();
            log.warn("Permission denied: user={} required={}:{}:{}", username, resource, action, scope);
            auditService.denied(
                    SecurityUtils.getCurrentUserId(),
                    username,
                    resource,
                    null,
                    action,
                    Map.of("requiredScope", scope, "method", pjp.getSignature().toShortString())
            );
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        return pjp.proceed();
    }

    private boolean hasMatchingAuthority(Authentication auth, String resource, String action, String scope) {
        String exactPermission = resource + ":" + action + ":" + scope;
        String anyScope = resource + ":" + action + ":ANY";

        for (GrantedAuthority authority : auth.getAuthorities()) {
            String a = authority.getAuthority();
            if (a.equals(exactPermission) || a.equals(anyScope)) {
                return true;
            }
            // If scope is ANY, any matching resource:action:<any_scope> is sufficient
            if ("ANY".equals(scope) && a.startsWith(resource + ":" + action + ":")) {
                return true;
            }
        }
        return false;
    }
}
