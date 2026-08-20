package dev.reagentic.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferAuthVerifierTest {

    private static final String SECRET = "test-secret-0123456789-0123456789-0123456789";

    @Test
    void validAuthorizationPasses() {
        String token = TransferAuthVerifier.issue(SECRET, "user1", "txn-1");
        assertDoesNotThrow(() -> TransferAuthVerifier.verify(SECRET, token, "user1", "txn-1"));
    }

    @Test
    void missingAuthorizationRejected() {
        assertThrows(TransferForbiddenException.class,
                () -> TransferAuthVerifier.verify(SECRET, null, "user1", "txn-1"));
        assertThrows(TransferForbiddenException.class,
                () -> TransferAuthVerifier.verify(SECRET, "", "user1", "txn-1"));
        assertThrows(TransferForbiddenException.class,
                () -> TransferAuthVerifier.verify(SECRET, "   ", "user1", "txn-1"));
    }

    @Test
    void wrongRoleRejected() {
        String userToken = JwtUtil.issue(SECRET, "user1", "USER", 60_000);
        assertThrows(TransferForbiddenException.class,
                () -> TransferAuthVerifier.verify(SECRET, userToken, "user1", "txn-1"));
    }

    @Test
    void wrongSubjectRejected() {
        String token = TransferAuthVerifier.issue(SECRET, "user2", "txn-1");
        assertThrows(TransferForbiddenException.class,
                () -> TransferAuthVerifier.verify(SECRET, token, "user1", "txn-1"));
    }

    @Test
    void wrongTransferKeyRejected() {
        String token = TransferAuthVerifier.issue(SECRET, "user1", "txn-1");
        assertThrows(TransferForbiddenException.class,
                () -> TransferAuthVerifier.verify(SECRET, token, "user1", "txn-2"));
    }

    @Test
    void expiredAuthorizationRejected() {
        String expired = TransferAuthVerifier.issue(SECRET, "user1", "txn-1");
        // Force expiry by waiting past the TTL is impractical; instead verify that a
        // token minted with a negative TTL (already expired) is rejected.
        String alreadyExpired = JwtUtil.issue(SECRET, "user1", TransferAuthVerifier.ROLE, -1_000,
                java.util.Map.of(TransferAuthVerifier.TXN_CLAIM, "txn-1"));
        assertThrows(TransferForbiddenException.class,
                () -> TransferAuthVerifier.verify(SECRET, alreadyExpired, "user1", "txn-1"));
        assertDoesNotThrow(() -> TransferAuthVerifier.verify(SECRET, expired, "user1", "txn-1"));
    }

    @Test
    void tamperedAuthorizationRejected() {
        String token = TransferAuthVerifier.issue(SECRET, "user1", "txn-1");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThrows(TransferForbiddenException.class,
                () -> TransferAuthVerifier.verify(SECRET, tampered, "user1", "txn-1"));
    }
}