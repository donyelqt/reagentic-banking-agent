package dev.reagentic.notification.repository;

import dev.reagentic.notification.domain.NotificationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationRecord, Long> {
    boolean existsByPaymentId(String paymentId);
}
