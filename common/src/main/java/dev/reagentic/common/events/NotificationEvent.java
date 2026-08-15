package dev.reagentic.common.events;

import dev.reagentic.common.money.Money;

import java.time.Instant;

/**
 * Notification trigger consumed by notification-service (email/SMS stub).
 */
public record NotificationEvent(
        String notificationId,
        String userId,
        String channel,   // EMAIL | SMS
        String subject,
        String body,
        long timestamp) {

    public NotificationEvent {
        if (timestamp == 0) timestamp = Instant.now().toEpochMilli();
    }

    public NotificationEvent(String notificationId, String userId, String channel, String subject, String body) {
        this(notificationId, userId, channel, subject, body, Instant.now().toEpochMilli());
    }
}
