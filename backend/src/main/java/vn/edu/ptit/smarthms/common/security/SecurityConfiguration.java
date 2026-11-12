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
 * Stateless security based on the SYS account tables (merged from the phung-nam branch).
 * <ul>
 *   <li>POST /api/v1/auth/login checks username/password (BCrypt) against sys.user_account
 *       and returns an opaque bearer token; only its SHA-256 digest is stored in sys.session.</li>
 *   <li>Every other /api/v1 call must send "Authorization: Bearer &lt;token&gt;".</li>
 * </ul>
 * Module rules are still enforced inside the services through the {@link Actor}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    private final SmartHmsUserDetailsService userDetailsService;

    public SecurityConfiguration(SmartHmsUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

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
