package dev.reagentic.auth.config;

import dev.reagentic.common.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${JWT_SECRET}")
    private String secret;

    @Value("${JWT_TTL_MS:86400000}")
    private long ttlMillis;

    public String issue(String subject, String role) {
        return JwtUtil.issue(secret, subject, role, ttlMillis);
    }

    public Claims verify(String token) {
        return JwtUtil.verify(secret, token);
    }
}
