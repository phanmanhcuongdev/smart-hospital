package vn.edu.ptit.smarthms.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.ptit.smarthms.common.audit.AuditService;
import vn.edu.ptit.smarthms.common.error.AppException;

import java.io.IOException;
import java.util.Map;

/**
 * Reads the "Authorization: Bearer <token>" header and authenticates the request
 * by looking up the opaque token from sys.session.
 */
public class BearerTokenFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTR = "SMARTHMS_AUTH_ERROR_CODE";
    public static final String AUTH_ERROR_MSG_ATTR = "SMARTHMS_AUTH_ERROR_MSG";
    private static final Logger log = LoggerFactory.getLogger(BearerTokenFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final SmartHmsUserDetailsService userDetailsService;
    private final AuditService auditService;

    public BearerTokenFilter(SmartHmsUserDetailsService userDetailsService) {
        this(userDetailsService, null);
    }

    public BearerTokenFilter(SmartHmsUserDetailsService userDetailsService, AuditService auditService) {
        this.userDetailsService = userDetailsService;
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String rawToken = authHeader.substring(BEARER_PREFIX.length()).trim();
            try {
                UserDetails userDetails = userDetailsService.loadUserByToken(rawToken);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                if (userDetails instanceof SmartHmsUserDetails sud) {
                    authentication.setDetails(sud.getUserId());
                }

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AppException ex) {
                log.debug("Token validation rejected with code={}", ex.getErrorCode().getCode());
                request.setAttribute(AUTH_ERROR_ATTR, ex.getErrorCode());
                request.setAttribute(AUTH_ERROR_MSG_ATTR, ex.getMessage());
                if (auditService != null) {
                    auditService.denied(
                            null,
                            "session_bearer",
                            "SYS_SESSION",
                            request.getRequestURI(),
                            "AUTHENTICATE_TOKEN",
                            Map.of("reason", ex.getErrorCode().getCode(), "path", request.getRequestURI())
                    );
                }
            } catch (Exception ex) {
                log.debug("Token validation failed: {}", ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
