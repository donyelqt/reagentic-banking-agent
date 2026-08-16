package dev.reagentic.ai.config;

import dev.reagentic.common.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (request.getServletPath().startsWith("/actuator/")) {
            chain.doFilter(request, response);
            return;
        }
        String token = JwtUtil.bearer(request.getHeader("Authorization"));
        if (token == null || token.isBlank()) {
            log.warn("jwt-filter MISSING token for {}", request.getServletPath());
            sendError(response, HttpStatus.UNAUTHORIZED, "MISSING_TOKEN", "Authorization header required");
            return;
        }
        try {
            Claims claims = JwtUtil.verify(jwtSecret, token);
            String role = claims.get("role", String.class);
            var auth = new UsernamePasswordAuthenticationToken(
                    claims.getSubject(), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + (role == null ? "USER" : role))));
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.warn("jwt-filter VERIFY FAILED for {}: {} (token head {})",
                    request.getServletPath(), e.getMessage(),
                    token.substring(0, Math.min(20, token.length())));
            sendError(response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token verification failed");
        }
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"status\":" + status.value() + ",\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
