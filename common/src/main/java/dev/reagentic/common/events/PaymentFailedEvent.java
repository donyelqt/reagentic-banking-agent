package dev.reagentic.common.events;

import dev.reagentic.common.money.Money;

import java.time.Instant;

/**
 * Immutable audit event: a transfer failed. When debitApplied is true the debit
 * had already been applied and was compensated (credit leg failed); the ledger
 * records the DEBIT_FAILED/COMPENSATE pair to keep balances correct. When false
 * the debit itself was rejected (e.g. insufficient funds) and nothing moved, so
 * the ledger records nothing.
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
        long timestamp) {

    public PaymentFailedEvent {
        if (timestamp == 0) timestamp = Instant.now().toEpochMilli();
    }

    public PaymentFailedEvent(String paymentId, String sourceAccountId, Money amount, String currency,
                              String reason, String idempotencyKey, long timestamp) {
        this("PaymentFailed", paymentId, sourceAccountId, amount, currency, reason, idempotencyKey, false, timestamp);
    }

    public PaymentFailedEvent(String paymentId, String sourceAccountId, Money amount, String currency,
                              String reason, String idempotencyKey, boolean debitApplied, long timestamp) {
        this("PaymentFailed", paymentId, sourceAccountId, amount, currency, reason, idempotencyKey, debitApplied, timestamp);
    }
}
