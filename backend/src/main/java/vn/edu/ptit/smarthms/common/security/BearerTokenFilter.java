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

import java.io.IOException;

/**
 * Reads the "Authorization: Bearer <token>" header and authenticates the request
 * by looking up the opaque token from sys.session.
 */
public class BearerTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BearerTokenFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final SmartHmsUserDetailsService userDetailsService;

    public BearerTokenFilter(SmartHmsUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
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
            } catch (Exception ex) {
                log.debug("Token validation failed: {}", ex.getMessage());
                // Don't set authentication — security config will reject unauthenticated requests
            }
        }

        filterChain.doFilter(request, response);
    }
}
