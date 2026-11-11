package vn.edu.ptit.smarthms.common.security;

import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import vn.edu.ptit.smarthms.common.logging.CorrelationContext;
import vn.edu.ptit.smarthms.common.logging.CorrelationLoggingFilter;

/**
 * Demo-only security: HTTP Basic with in-memory demo users whose shared
 * password comes from SMARTHMS_DEMO_PASSWORD, default demo. The SYS
 * account service (V.2) must replace this before any real deployment.
 * Module rules are enforced again inside the services via the Actor.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    public UserDetailsService demoUsers(PasswordEncoder passwordEncoder) {
        String password = System.getenv("SMARTHMS_DEMO_PASSWORD");
        if (password == null || password.isBlank()) {
            password = "demo";
        }
        String encoded = passwordEncoder.encode(password);
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        users.createUser(User.withUsername("EMP-DEMO-001").password(encoded).roles("DOCTOR").build());
        users.createUser(User.withUsername("demo-nurse").password(encoded).roles("NURSE").build());
        users.createUser(User.withUsername("demo-cls").password(encoded).roles("CLS_TECHNICIAN").build());
        users.createUser(User.withUsername("demo-records").password(encoded).roles("RECORDS_MANAGER").build());
        users.createUser(User.withUsername("demo-reception").password(encoded).roles("RECEPTION").build());
        users.createUser(User.withUsername("demo-hr").password(encoded).roles("HR_SPECIALIST").build());
        users.createUser(User.withUsername("demo-hr-approver").password(encoded).roles("HR_APPROVER").build());
        users.createUser(User.withUsername("demo-admin").password(encoded).roles("ADMIN", "DOCTOR", "NURSE", "HR_SPECIALIST", "HR_APPROVER", "RECORDS_MANAGER").build());
        return users;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/api/v1/system/health").permitAll()
                        .requestMatchers("/api/v1/emr/**", "/api/v1/hr/**").authenticated()
                        .anyRequest().denyAll())
                .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    String correlationId = CorrelationContext.getCorrelationId();
                    response.setHeader(CorrelationLoggingFilter.CORRELATION_ID_HEADER, correlationId);
                    String json = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Full authentication is required to access this resource\",\"correlationId\":\""
                            + correlationId + "\",\"timestamp\":\"" + Instant.now() + "\"}";
                    response.getWriter().write(json);
                }))
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
