package dev.reagentic.common.events;

import dev.reagentic.common.money.Money;

import java.time.Instant;

/**
 * Immutable audit event: a transfer failed (e.g. insufficient funds). The
 * ledger applies a source -/source + compensation so balances stay correct.
 */
public record PaymentFailedEvent(
        String eventType,
        String paymentId,
        String sourceAccountId,
        Money amount,
        String currency,
        String reason,
        String idempotencyKey,
        long timestamp) {

    public PaymentFailedEvent {
        if (timestamp == 0) timestamp = Instant.now().toEpochMilli();
    }

    public PaymentFailedEvent(String paymentId, String sourceAccountId, Money amount, String currency,
                              String reason, String idempotencyKey, long timestamp) {
        this("PaymentFailed", paymentId, sourceAccountId, amount, currency, reason, idempotencyKey, timestamp);
    }
}
