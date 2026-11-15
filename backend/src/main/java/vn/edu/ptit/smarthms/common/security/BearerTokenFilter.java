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
 * Mô tả: Bộ lọc (Filter) chặn mọi request để trích xuất header "Authorization: Bearer <token>", 
 *        xác thực token qua TokenService/UserDetailsService và thiết lập SecurityContext cho phiên làm việc
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
public class BearerTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BearerTokenFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final SmartHmsUserDetailsService userDetailsService;

    public BearerTokenFilter(SmartHmsUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * Mô tả: Kiểm tra và giải mã Bearer token từ request header, nạp quyền người dùng vào SecurityContextHolder
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
     */
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
                // Don't set authentication: the security config rejects unauthenticated requests
            }
        }

        filterChain.doFilter(request, response);
    }
}
