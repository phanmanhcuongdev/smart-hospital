package vn.edu.ptit.smarthms.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

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
                .httpBasic(httpBasic -> { })
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
