package dev.reagentic.common.events;

import dev.reagentic.common.money.Money;

import java.time.Instant;

/**
 * Emitted when an account is opened. Seeds the ledger opening entry.
 */
public record AccountOpenedEvent(
        String accountId,
        String userId,
        String type,
        Money openingBalance,
        long timestamp) {

    public AccountOpenedEvent {
        if (timestamp == 0) timestamp = Instant.now().toEpochMilli();
    }

    public AccountOpenedEvent(String accountId, String userId, String type, Money openingBalance) {
        this(accountId, userId, type, openingBalance, Instant.now().toEpochMilli());
    }
}
