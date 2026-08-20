package dev.reagentic.common.security;

import io.jsonwebtoken.Claims;

import java.util.Map;

/**
 * Approval boundary for money movement, shared by the ai-agent (mints) and
 * payment-service (verifies). A transfer may only execute when the caller
 * presents a short-lived, signed authorization token that the ai-agent mints
 * ONLY after an approved transferFunds step has passed the executor's approval
 * gate. Without it, a direct call to /api/payments/transfer is rejected —
 * approval is a server-enforced boundary, not a UI convention.
 *
 * The token is signed with the shared JWT secret (the same trust anchor every
 * service already verifies), scoped to the caller's subject and to the exact
 * transfer idempotency key, and expires in a minute.
 */
public final class TransferAuthVerifier {

    public static final String ROLE = "TRANSFER";
    public static final String TXN_CLAIM = "txn";
    public static final long TTL_MILLIS = 60_000L;

    private TransferAuthVerifier() {
    }

    public static String issue(String secret, String subject, String transferKey) {
        return JwtUtil.issue(secret, subject, ROLE, TTL_MILLIS, Map.of(TXN_CLAIM, transferKey));
    }

    public static void verify(String secret, String token, String callerSubject, String transferKey) {
        if (token == null || token.isBlank()) {
            throw new TransferForbiddenException("transfer requires an agent-approved authorization");
        }
        Claims claims;
        try {
            claims = JwtUtil.verify(secret, token);
        } catch (Exception e) {
            throw new TransferForbiddenException("invalid transfer authorization");
        }
        if (!ROLE.equals(claims.get("role", String.class))) {
            throw new TransferForbiddenException("invalid transfer authorization");
        }
        if (!transferKey.equals(claims.get(TXN_CLAIM, String.class))) {
            throw new TransferForbiddenException("transfer authorization does not match this transfer");
        }
        if (!callerSubject.equals(claims.getSubject())) {
            throw new TransferForbiddenException("transfer authorization belongs to another user");
        }
    }
}