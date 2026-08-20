package dev.reagentic.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reagentic.common.events.PaymentCompletedEvent;
import dev.reagentic.common.events.PaymentFailedEvent;
import dev.reagentic.common.money.Money;
import dev.reagentic.common.security.JwtUtil;
import dev.reagentic.payment.domain.Outbox;
import dev.reagentic.payment.domain.Payment;
import dev.reagentic.payment.domain.PaymentStatus;
import dev.reagentic.payment.repository.OutboxRepository;
import dev.reagentic.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final RestClient accountClient;
    private final ObjectMapper objectMapper;

    @Value("${SERVICE_TOKEN:}")
    private String serviceToken;

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    public PaymentService(PaymentRepository paymentRepository, OutboxRepository outboxRepository,
                          RestClient accountClient, ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.accountClient = accountClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Transfer saga: debit source -> credit destination -> write outbox.
     * Idempotent per transfer key; each leg uses a DISTINCT idempotency key so a
     * retry can never double-apply (debit and credit are never conflated).
     */
    @Transactional
    public Payment transfer(String userToken, String source, String dest, Money amount, String transferKey) {
        Optional<Payment> existing = paymentRepository.findById(transferKey);
        if (existing.isPresent()) {
            Payment p = existing.get();
            if (p.getStatus() == PaymentStatus.COMPLETED) {
                return p;
            }
            if (p.getStatus() == PaymentStatus.FAILED) {
                throw new PaymentFailedException(p.getReason());
            }
        }

        Payment payment = existing.orElseGet(() -> paymentRepository.save(
                new Payment(transferKey, source, dest, amount.value(), "USD", PaymentStatus.PENDING)));

        String debitKey = transferKey + ":debit";
        String creditKey = transferKey + ":credit";
        String compensateKey = transferKey + ":compensate";

        try {
            callAccount("/api/accounts/internal/debit", source, amount, debitKey, userToken);
        } catch (Exception e) {
            payment.fail("debit failed: " + e.getMessage());
            paymentRepository.save(payment);
            writeFailedOutbox(payment, amount, payment.getReason(), transferKey, false);
            return payment;
        }

        try {
            callAccount("/api/accounts/internal/credit", dest, amount, creditKey, userToken);
        } catch (Exception e) {
            // Compensate the already-applied debit by crediting the source back.
            try {
                callAccount("/api/accounts/internal/credit", source, amount, compensateKey, userToken);
            } catch (Exception ce) {
                // best-effort compensation failed; still record the failure
            }
            payment.fail("credit failed, compensated: " + e.getMessage());
            paymentRepository.save(payment);
            writeFailedOutbox(payment, amount, payment.getReason(), transferKey, true);
            return payment;
        }

        payment.complete();
        paymentRepository.save(payment);
        writeCompletedOutbox(payment, amount, transferKey);
        return payment;
    }

    private void callAccount(String path, String accountId, Money amount, String idempotencyKey, String userToken) {
        String subject = "";
        try {
            String raw = JwtUtil.bearer(userToken);
            if (raw != null && !raw.isBlank()) {
                subject = JwtUtil.verify(jwtSecret, raw).getSubject();
            }
        } catch (Exception e) {
            // fall back to empty subject; account-service will reject the call
        }
        var response = accountClient.post()
                .uri(path)
                .header("Authorization", userToken)
                .header("X-Service-Token", serviceToken)
                .header("X-User-Subject", subject)
                .body(new AccountMutateRequest(accountId, amount.asString(), idempotencyKey))
                .retrieve()
                .toBodilessEntity();
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AccountCallException("account call " + path + " failed: " + response.getStatusCode());
        }
    }

    private void writeCompletedOutbox(Payment p, Money amount, String transferKey) {
        try {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    p.getPaymentId(), p.getSourceAccountId(), p.getDestinationAccountId(),
                    amount, p.getCurrency(), transferKey, System.currentTimeMillis());
            outboxRepository.save(new Outbox(p.getPaymentId(), "PaymentCompleted",
                    objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize outbox", e);
        }
    }

    private void writeFailedOutbox(Payment p, Money amount, String reason, String transferKey, boolean debitApplied) {
        try {
            PaymentFailedEvent event = new PaymentFailedEvent(
                    p.getPaymentId(), p.getSourceAccountId(), amount,
                    p.getCurrency(), reason, transferKey, debitApplied, System.currentTimeMillis());
            outboxRepository.save(new Outbox(p.getPaymentId(), "PaymentFailed",
                    objectMapper.writeValueAsString(event)));
        } catch (Exception e) {
            throw new IllegalStateException("failed to serialize outbox", e);
        }
    }

    public record AccountMutateRequest(String accountId, String amount, String idempotencyKey) {
    }

    public static class PaymentFailedException extends RuntimeException {
        public PaymentFailedException(String message) {
            super(message);
        }
    }

    public static class AccountCallException extends RuntimeException {
        public AccountCallException(String message) {
            super(message);
        }
    }
}
