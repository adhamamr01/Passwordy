package com.adhamamr.passwordy.config;

import com.adhamamr.passwordy.security.ClientIpResolver;
import com.adhamamr.passwordy.security.JwtAuthenticationEntryPoint;
import com.adhamamr.passwordy.security.JwtAuthenticationFilter;
import com.adhamamr.passwordy.security.RateLimitFilter;
import com.adhamamr.passwordy.security.RateLimitingService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central Spring Security setup: stateless JWT auth with CSRF disabled.
 *
 * <p>Auth and password-generation routes are public; everything else requires a valid
 * token. Sessions are disabled ({@code STATELESS}) so the JWT is the sole credential, and
 * {@link JwtAuthenticationFilter} runs ahead of the username/password filter to populate
 * the security context from the bearer token.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final RateLimitProperties rateLimitProperties;
    private final RateLimitingService rateLimitingService;
    private final ClientIpResolver clientIpResolver;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          RateLimitProperties rateLimitProperties,
                          RateLimitingService rateLimitingService,
                          ClientIpResolver clientIpResolver) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.rateLimitProperties = rateLimitProperties;
        this.rateLimitingService = rateLimitingService;
        this.clientIpResolver = clientIpResolver;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection is intentionally disabled: this is a stateless API
                // (SessionCreationPolicy.STATELESS) authenticated by a bearer JWT in the
                // Authorization header, not by cookies. CSRF requires the browser to auto-attach
                // ambient credentials (cookies) to a forged cross-site request — there are none
                // here, so there is nothing to forge. (CodeQL java/spring-disabled-csrf-protection
                // is a false positive for header-token APIs.)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()  // register/login
                        .requestMatchers("/api/password/generate", "/api/password/generate-pin").permitAll()  // generation utilities
                        .anyRequest().authenticated()  // everything else, incl. /api/password/categories
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // No sessions, using JWT
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))  // 401 JSON instead of default
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // After JWT auth so the authenticated tier can key by username; still ahead of the controller.
                .addFilterAfter(
                        new RateLimitFilter(rateLimitProperties, rateLimitingService, clientIpResolver),
                        JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // OWASP-recommended defaults: 19 MiB memory, 2 iterations, 1 thread
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}