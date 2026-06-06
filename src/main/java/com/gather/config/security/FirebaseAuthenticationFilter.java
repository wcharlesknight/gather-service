package com.gather.config.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies a Firebase ID token from the "Authorization: Bearer ..." header and, on success,
 * populates the SecurityContext with the user's uid as principal and authorities derived from
 * custom claims (an "admin" claim grants ROLE_ADMIN).
 *
 * <p>The filter never rejects requests itself: a missing or invalid token simply leaves the
 * context unauthenticated, so the authorization rules + entry point decide the outcome. This
 * keeps public routes (e.g. /api/auth/login, which verifies the token itself) working even when
 * the presented token cannot be verified here.
 */
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (firebaseAuth != null && header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                // checkRevoked=true so signed-out / disabled / revoked sessions are rejected (H5).
                FirebaseToken decoded = firebaseAuth.verifyIdToken(token, true);
                if (decoded != null) {
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    if (Boolean.TRUE.equals(decoded.getClaims().get("admin"))) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(decoded.getUid(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // Invalid/expired/revoked token: stay unauthenticated and let authorization decide.
                logger.debug("Firebase token verification failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
