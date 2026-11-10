package vn.edu.ptit.smarthms.emr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** TẠM THỜI cho dev V.3. XÓA khi V.2 (auth/RBAC) hoàn thành. */
@Configuration
public class EmrDevSecurityConfiguration {
    @Bean
    @Order(1)
    SecurityFilterChain emrDevChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/v1/emr/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .build();
    }
}