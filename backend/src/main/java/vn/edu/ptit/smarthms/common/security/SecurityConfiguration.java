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

    public SecurityConfiguration(SmartHmsUserDetailsService userDetailsService,
                                 RequestAuditFilter requestAuditFilter) {
        this.userDetailsService = userDetailsService;
        this.requestAuditFilter = requestAuditFilter;
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
        return new BearerTokenFilter(userDetailsService);
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
                                "/api/v1/auth/login"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((request, response, authException) -> {
                            writeJsonError(response, ErrorCode.AUTH_TOKEN_INVALID);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            writeJsonError(response, ErrorCode.ACCESS_DENIED);
                        })
                )
                .addFilterBefore(requestAuditFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(bearerTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authenticationProvider(authenticationProvider())
                .build();
    }

    private void writeJsonError(HttpServletResponse response, ErrorCode errorCode) throws java.io.IOException {
        String cid = CorrelationIdHolder.get();
        if (cid == null || cid.isBlank()) {
            cid = UUID.randomUUID().toString();
        }
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = String.format(
                "{\"code\":\"%s\",\"message\":\"%s\",\"correlationId\":\"%s\",\"timestamp\":\"%s\",\"details\":null}",
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                cid,
                Instant.now().toString()
        );
        response.getWriter().write(body);
    }
}
