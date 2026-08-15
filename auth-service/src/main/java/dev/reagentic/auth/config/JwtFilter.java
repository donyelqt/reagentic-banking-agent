package dev.reagentic.auth.config;

import dev.reagentic.common.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

/**
 * Zero-internal-trust JWT filter: every request (except login/health) must carry
 * a valid token signed with the shared secret. The caller identity + role are
 * reconstructed and placed in the SecurityContext for downstream authorization.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.startsWith("/api/auth/login") || path.startsWith("/actuator/")) {
            chain.doFilter(request, response);
            return;
        }
        String token = JwtUtil.bearer(request.getHeader("Authorization"));
        if (token == null || token.isBlank()) {
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
