package com.gather.config.security;

import com.google.firebase.auth.FirebaseAuth;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Stateless, token-based security for the REST API.
 *
 * <p>Public:        /api/health, /api/auth/**, GET /api/cities[/**], GET /api/gathering-spots/**
 * <p>Admin only:    /api/admin/**, and city writes (POST /api/cities, PUT/DELETE /api/cities/**)
 * <p>Authenticated: everything else (notably /api/users/**, /api/places/**)
 *
 * <p>Admin access requires a Firebase user with a custom claim {@code admin=true}; set it via the
 * Firebase Admin SDK ({@code FirebaseAuth.setCustomUserClaims(uid, Map.of("admin", true))}).
 */
@Configuration
public class SecurityConfig {

    @Value("${security.cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ObjectProvider<FirebaseAuth> firebaseAuth)
            throws Exception {
        // Constructed here (not exposed as a @Bean) so Spring Boot does not also auto-register it
        // as a top-level servlet filter. ObjectProvider tolerates Firebase being disabled.
        FirebaseAuthenticationFilter firebaseFilter =
                new FirebaseAuthenticationFilter(firebaseAuth.getIfAvailable());
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cities", "/api/cities/**",
                                "/api/gathering-spots/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/cities").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/cities/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/cities/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**", "/api/places/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> writeError(res, HttpServletResponse.SC_UNAUTHORIZED,
                                "Authentication required."))
                        .accessDeniedHandler((req, res, e) -> writeError(res, HttpServletResponse.SC_FORBIDDEN,
                                "Insufficient privileges.")))
                .addFilterBefore(firebaseFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
        }
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
