package dev.reagentic.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Hand-rolled JWT helper (JJWT). The SAME secret is used to sign (auth-service)
 * and verify (every other service + gateway). Zero internal trust: every
 * service re-verifies the caller's token on every request.
 */
public final class JwtUtil {

    private JwtUtil() {
    }

    private static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String issue(String secret, String subject, String role, long ttlMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis))
                .signWith(key(secret))
                .compact();
    }

    public static Claims verify(String secret, String token) {
        return Jwts.parser()
                .verifyWith(key(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Extracts the raw token from an Authorization header (handles "Bearer "). */
    public static String bearer(String header) {
        if (header == null) return null;
        String h = header.trim();
        if (h.toLowerCase().startsWith("bearer ")) {
            return h.substring(7).trim();
        }
        return h;
    }
}
