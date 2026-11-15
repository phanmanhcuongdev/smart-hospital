package vn.edu.ptit.smarthms.common.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.edu.ptit.smarthms.common.logging.CorrelationContext;
import vn.edu.ptit.smarthms.common.logging.CorrelationLoggingFilter;

/**
 * Mô tả: Cấu hình bảo mật hệ thống (Spring Security) - kiểm soát truy cập phi trạng thái (Stateless),
 *        đăng ký bộ lọc BearerTokenFilter, phân quyền theo URL và chuẩn hóa phản hồi lỗi xác thực (401/403)
 * Người thực hiện: Đoàn Thị Hồng Nhung
 * Ngày thực hiện: 12/11/2026
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    private final SmartHmsUserDetailsService userDetailsService;

    public SecurityConfiguration(SmartHmsUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * Mô tả: Khởi tạo bộ mã hóa mật khẩu sử dụng BCrypt (độ phức tạp 12)
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Mô tả: Cấu hình AuthenticationManager sử dụng DaoAuthenticationProvider kết nối với SmartHmsUserDetailsService
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
     */
    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * Mô tả: Cấu hình chuỗi lọc bảo mật SecurityFilterChain, cho phép truy cập public vào endpoint login/health
     *        và yêu cầu xác thực Bearer token cho tất cả các API nghiệp vụ khác
     * Người thực hiện: Đoàn Thị Hồng Nhung
     * Ngày thực hiện: 12/11/2026
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/api/v1/system/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJsonError(response, HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_INVALID",
                                        "Missing, invalid or expired access token. Please sign in again."))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJsonError(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                                        "You do not have access to this resource.")))
                .addFilterBefore(new BearerTokenFilter(userDetailsService), UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    private static void writeJsonError(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        String correlationId = CorrelationContext.getCorrelationId();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(CorrelationLoggingFilter.CORRELATION_ID_HEADER, correlationId);
        String json = "{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"correlationId\":\""
                + correlationId + "\",\"timestamp\":\"" + Instant.now() + "\"}";
        response.getWriter().write(json);
    }
}
