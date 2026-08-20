package dev.reagentic.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.reagentic.common.money.Money;

import java.time.Instant;

/**
 * Immutable audit event: a transfer failed. When debitApplied is true the debit
 * had already been applied (credit leg failed); the ledger records the debit.
 * When compensateApplied is also true the source was credited back, so the
 * ledger records the DEBIT_FAILED/COMPENSATE pair (net zero). When the
 * compensation itself failed, only the DEBIT_FAILED leg is recorded so the
 * ledger still matches the account, which remains debited. When debitApplied is
 * false the debit itself was rejected (e.g. insufficient funds) and nothing
 * moved, so the ledger records nothing.
 */
public record PaymentFailedEvent(
        String eventType,
        String paymentId,
        String sourceAccountId,
        Money amount,
        String currency,
        String reason,
        String idempotencyKey,
        boolean debitApplied,
        boolean compensateApplied,
        long timestamp) {

    public PaymentFailedEvent {
        if (timestamp == 0) timestamp = Instant.now().toEpochMilli();
    }

    /**
     * Legacy events predate compensateApplied; in that version debitApplied
     * meant the debit had been applied AND compensated. Missing
     * compensateApplied therefore defaults to debitApplied so in-flight events
     * during a rolling upgrade keep their original meaning.
     */
    @JsonCreator
    public static PaymentFailedEvent from(
            @JsonProperty("eventType") String eventType,
            @JsonProperty("paymentId") String paymentId,
            @JsonProperty("sourceAccountId") String sourceAccountId,
            @JsonProperty("amount") Money amount,
            @JsonProperty("currency") String currency,
            @JsonProperty("reason") String reason,
            @JsonProperty("idempotencyKey") String idempotencyKey,
            @JsonProperty("debitApplied") Boolean debitApplied,
            @JsonProperty("compensateApplied") Boolean compensateApplied,
            @JsonProperty("timestamp") long timestamp) {
        boolean debit = debitApplied != null && debitApplied;
        boolean compensated = compensateApplied != null ? compensateApplied : debit;
        return new PaymentFailedEvent(eventType, paymentId, sourceAccountId, amount, currency, reason,
                idempotencyKey, debit, compensated, timestamp);
    }

    public PaymentFailedEvent(String paymentId, String sourceAccountId, Money amount, String currency,
                              String reason, String idempotencyKey, long timestamp) {
        this("PaymentFailed", paymentId, sourceAccountId, amount, currency, reason, idempotencyKey, false, false, timestamp);
    }

    public PaymentFailedEvent(String paymentId, String sourceAccountId, Money amount, String currency,
                              String reason, String idempotencyKey, boolean debitApplied, long timestamp) {
        this("PaymentFailed", paymentId, sourceAccountId, amount, currency, reason, idempotencyKey, debitApplied, debitApplied, timestamp);
    }

    public PaymentFailedEvent(String paymentId, String sourceAccountId, Money amount, String currency,
                              String reason, String idempotencyKey, boolean debitApplied, boolean compensateApplied, long timestamp) {
        this("PaymentFailed", paymentId, sourceAccountId, amount, currency, reason, idempotencyKey, debitApplied, compensateApplied, timestamp);
    }
}
