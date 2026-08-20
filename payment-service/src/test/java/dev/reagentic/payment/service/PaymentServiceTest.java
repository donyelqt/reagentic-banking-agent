package dev.reagentic.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reagentic.common.events.PaymentFailedEvent;
import dev.reagentic.common.money.Money;
import dev.reagentic.common.security.JwtUtil;
import dev.reagentic.common.security.TransferAuthVerifier;
import dev.reagentic.common.security.TransferForbiddenException;
import dev.reagentic.payment.domain.Outbox;
import dev.reagentic.payment.domain.Payment;
import dev.reagentic.payment.domain.PaymentStatus;
import dev.reagentic.payment.repository.OutboxRepository;
import dev.reagentic.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private void stubAccountChain(boolean debitFails, boolean creditFails, boolean compensationFails) {
        RestClient.RequestBodyUriSpec post = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodyUriSpec debitSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodyUriSpec creditSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec debitResponse = mock(RestClient.ResponseSpec.class);
        RestClient.ResponseSpec creditResponse = mock(RestClient.ResponseSpec.class);

        when(accountClient.post()).thenReturn(post);
        when(post.uri("/api/accounts/internal/debit")).thenReturn(debitSpec);
        when(post.uri("/api/accounts/internal/credit")).thenReturn(creditSpec);
        doAnswer(inv -> inv.getMock()).when(debitSpec).header(anyString(), any(String[].class));
        doAnswer(inv -> inv.getMock()).when(creditSpec).header(anyString(), any(String[].class));
        doAnswer(inv -> inv.getMock()).when(debitSpec).body(any(PaymentService.AccountMutateRequest.class));
        doAnswer(inv -> inv.getMock()).when(creditSpec).body(any(PaymentService.AccountMutateRequest.class));
        when(debitSpec.retrieve()).thenReturn(debitResponse);
        when(creditSpec.retrieve()).thenReturn(creditResponse);

        HttpClientErrorException error = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "400 Bad Request", HttpHeaders.EMPTY, new byte[0], null);
        when(debitResponse.toBodilessEntity()).thenAnswer(inv -> {
            if (debitFails) {
                throw error;
            }
            return ResponseEntity.ok().build();
        });
        when(creditResponse.toBodilessEntity()).thenAnswer(new Answer<ResponseEntity<Void>>() {
            private int calls = 0;

            @Override
            public ResponseEntity<Void> answer(InvocationOnMock inv) throws Throwable {
                calls++;
                if (calls == 1 && creditFails) {
                    throw error;
                }
                if (calls == 2 && compensationFails) {
                    throw error;
                }
                return ResponseEntity.ok().build();
            }
        });
    }

    private void stubIdempotentRepo() {
        when(paymentRepository.findById(anyString())).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private PaymentFailedEvent lastFailedOutboxEvent() throws Exception {
        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());
        return new ObjectMapper().readValue(captor.getValue().getPayload(), PaymentFailedEvent.class);
    }

    @Test
    void failedDebitRecordsNoDebitApplied() throws Exception {
        stubAccountChain(true, false, false);
        stubIdempotentRepo();

        Payment result = paymentService.transfer(USER_TOKEN, "acc-checking-0001", "acc-savings-0002",
                Money.of("50.00"), "k-debit-fail", authFor("k-debit-fail"));

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertFalse(lastFailedOutboxEvent().debitApplied());
    }

    @Test
    void failedCreditWithCompensationRecordsCompensationApplied() throws Exception {
        stubAccountChain(false, true, false);
        stubIdempotentRepo();

        Payment result = paymentService.transfer(USER_TOKEN, "acc-checking-0001", "acc-savings-0002",
                Money.of("50.00"), "k-comp-ok", authFor("k-comp-ok"));

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertTrue(result.getReason().contains("debit compensated"), "reason was: " + result.getReason());
        PaymentFailedEvent event = lastFailedOutboxEvent();
        assertTrue(event.debitApplied());
        assertTrue(event.compensateApplied());
    }

    @Test
    void failedCreditWithFailedCompensationRecordsSourceStaysDebited() throws Exception {
        stubAccountChain(false, true, true);
        stubIdempotentRepo();

        Payment result = paymentService.transfer(USER_TOKEN, "acc-checking-0001", "acc-savings-0002",
                Money.of("50.00"), "k-comp-fail", authFor("k-comp-fail"));

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertTrue(result.getReason().contains("compensation failed"), "reason was: " + result.getReason());
        PaymentFailedEvent event = lastFailedOutboxEvent();
        assertTrue(event.debitApplied());
        assertFalse(event.compensateApplied());
    }
}