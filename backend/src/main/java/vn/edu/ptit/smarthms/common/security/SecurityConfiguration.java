package vn.edu.ptit.smarthms.common.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.edu.ptit.smarthms.common.audit.AuditService;
import vn.edu.ptit.smarthms.common.audit.RequestAuditFilter;
import vn.edu.ptit.smarthms.common.error.ErrorCode;
import vn.edu.ptit.smarthms.common.util.CorrelationIdHolder;

import java.time.Instant;
import java.util.UUID;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    private final SmartHmsUserDetailsService userDetailsService;
    private final RequestAuditFilter requestAuditFilter;
    private final AuditService auditService;

    public SecurityConfiguration(SmartHmsUserDetailsService userDetailsService,
                                 RequestAuditFilter requestAuditFilter,
                                 AuditService auditService) {
        this.userDetailsService = userDetailsService;
        this.requestAuditFilter = requestAuditFilter;
        this.auditService = auditService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public BearerTokenFilter bearerTokenFilter() {
        return new BearerTokenFilter(userDetailsService, auditService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/api/v1/system/health",
                                "/api/v1/auth/login",
                                "/api/v1/sys/test-fixtures/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((request, response, authException) -> {
                            Object attr = request.getAttribute(BearerTokenFilter.AUTH_ERROR_ATTR);
                            ErrorCode code = (attr instanceof ErrorCode ec) ? ec : ErrorCode.AUTH_TOKEN_INVALID;
                            Object msgAttr = request.getAttribute(BearerTokenFilter.AUTH_ERROR_MSG_ATTR);
                            String msg = (msgAttr instanceof String s && !s.isBlank()) ? s : code.getDefaultMessage();
                            writeJsonError(response, code, msg);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            writeJsonError(response, ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getDefaultMessage());
                        })
                )
                .addFilterBefore(requestAuditFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(bearerTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authenticationProvider(authenticationProvider())
                .build();
    }

    private void writeJsonError(HttpServletResponse response, ErrorCode errorCode, String message) throws java.io.IOException {
        String cid = CorrelationIdHolder.get();
        if (cid == null || cid.isBlank()) {
            cid = UUID.randomUUID().toString();
        }
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String safeMsg = message != null ? message.replace("\"", "\\\"") : errorCode.getDefaultMessage();
        String body = String.format(
                "{\"code\":\"%s\",\"message\":\"%s\",\"correlationId\":\"%s\",\"timestamp\":\"%s\",\"details\":null}",
                errorCode.getCode(),
                safeMsg,
                cid,
                Instant.now().toString()
        );
        response.getWriter().write(body);
    }
}
