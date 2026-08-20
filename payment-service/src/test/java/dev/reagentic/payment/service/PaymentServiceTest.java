package dev.reagentic.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reagentic.common.money.Money;
import dev.reagentic.payment.repository.OutboxRepository;
import dev.reagentic.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentServiceTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final OutboxRepository outboxRepository = mock(OutboxRepository.class);
    private final RestClient accountClient = mock(RestClient.class);
    private final PaymentService paymentService =
            new PaymentService(paymentRepository, outboxRepository, accountClient, new ObjectMapper());

    @Test
    void selfTransferRejectedBeforeAnyPersistence() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.transfer("tok", "acc-checking-0001", "acc-checking-0001",
                        Money.of("100.00"), "k-1"),
                "must contain 'different'");

        verify(paymentRepository, never()).findById(anyString());
        verify(paymentRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(outboxRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blankAccountsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.transfer("tok", " ", "acc-savings-0002", Money.of("1.00"), "k-2"));
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.transfer("tok", "acc-checking-0001", null, Money.of("1.00"), "k-3"));
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.transfer("tok", null, "acc-savings-0002", Money.of("1.00"), "k-4"));

        verify(paymentRepository, never()).findById(anyString());
    }

    @Test
    void zeroAmountRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.transfer("tok", "acc-checking-0001", "acc-savings-0002",
                        Money.of("0.00"), "k-5"));

        verify(paymentRepository, never()).findById(anyString());
    }
}