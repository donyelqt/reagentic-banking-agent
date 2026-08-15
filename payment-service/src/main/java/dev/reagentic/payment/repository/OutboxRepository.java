package dev.reagentic.payment.repository;

import dev.reagentic.payment.domain.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findByPublishedFalse();
}
