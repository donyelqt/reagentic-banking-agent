package dev.reagentic.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reagentic.common.money.Money;
import dev.reagentic.common.security.JwtUtil;
import dev.reagentic.common.security.TransferAuthVerifier;
import dev.reagentic.common.security.TransferForbiddenException;
import dev.reagentic.payment.repository.OutboxRepository;
import dev.reagentic.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentServiceTest {

    private static final String SECRET = "test-secret-0123456789-0123456789-0123456789";
    private static final String USER_TOKEN = JwtUtil.issue(SECRET, "user1", "USER", 60_000);

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final OutboxRepository outboxRepository = mock(OutboxRepository.class);
    private final RestClient accountClient = mock(RestClient.class);
    private final PaymentService paymentService =
            new PaymentService(paymentRepository, outboxRepository, accountClient, new ObjectMapper());

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "jwtSecret", SECRET);
    }

    private String authFor(String key) {
        return TransferAuthVerifier.issue(SECRET, "user1", key);
    }

    @Test
    void transferWithoutAgentAuthorizationIsRejectedBeforeAnyPersistence() {
        assertThrows(TransferForbiddenException.class,
                () -> paymentService.transfer(USER_TOKEN, "acc-checking-0001", "acc-savings-0002",
                        Money.of("100.00"), "k-x", null));

        verify(paymentRepository, never()).findById(anyString());
        verify(paymentRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(outboxRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void selfTransferRejectedBeforeAnyPersistence() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.transfer(USER_TOKEN, "acc-checking-0001", "acc-checking-0001",
                        Money.of("100.00"), "k-1", authFor("k-1")),
                "must contain 'different'");

        verify(paymentRepository, never()).findById(anyString());
        verify(paymentRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(outboxRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blankAccountsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.transfer(USER_TOKEN, " ", "acc-savings-0002", Money.of("1.00"), "k-2", authFor("k-2")));
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.transfer(USER_TOKEN, "acc-checking-0001", null, Money.of("1.00"), "k-3", authFor("k-3")));
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.transfer(USER_TOKEN, null, "acc-savings-0002", Money.of("1.00"), "k-4", authFor("k-4")));

        verify(paymentRepository, never()).findById(anyString());
    }

    @Test
    void zeroAmountRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.transfer(USER_TOKEN, "acc-checking-0001", "acc-savings-0002",
                        Money.of("0.00"), "k-5", authFor("k-5")));

        verify(paymentRepository, never()).findById(anyString());
    }
}