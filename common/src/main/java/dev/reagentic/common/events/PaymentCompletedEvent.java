package dev.reagentic.common.events;

import dev.reagentic.common.money.Money;

import java.time.Instant;

/**
 * Immutable audit event: a transfer succeeded. Consumed by ledger-service
 * (source -, dest +) and notification-service (receipt).
 */
public record PaymentCompletedEvent(
        String eventType,
        String paymentId,
        String sourceAccountId,
        String destinationAccountId,
        Money amount,
        String currency,
        String idempotencyKey,
        long timestamp) {

    public PaymentCompletedEvent {
        if (timestamp == 0) timestamp = Instant.now().toEpochMilli();
    }

    public PaymentCompletedEvent(String paymentId, String sourceAccountId, String destinationAccountId,
                                 Money amount, String currency, String idempotencyKey, long timestamp) {
        this("PaymentCompleted", paymentId, sourceAccountId, destinationAccountId, amount, currency, idempotencyKey, timestamp);
    }
}
