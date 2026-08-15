package dev.reagentic.payment.service;

import dev.reagentic.payment.domain.Outbox;
import dev.reagentic.payment.repository.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox relay: publishes unpublished outbox rows to Kafka idempotently.
 * The event is marked published ONLY after a successful, acknowledged send, so
 * a crash mid-relay re-publishes on the next cycle (ledger dedupes by paymentId).
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final String TOPIC = "payment-events";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${outbox.relay.enabled:true}")
    private boolean relayEnabled;

    public OutboxRelay(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void relay() {
        if (!relayEnabled) {
            return;
        }
        List<Outbox> pending = outboxRepository.findByPublishedFalse();
        for (Outbox row : pending) {
            try {
                kafkaTemplate.send(new ProducerRecord<>(TOPIC, row.getAggregateId(), row.getPayload())).get();
                row.setPublished(true);
                outboxRepository.save(row);
            } catch (Exception e) {
                log.error("Outbox relay failed for aggregate {}: {}", row.getAggregateId(), e.getMessage());
            }
        }
    }
}
