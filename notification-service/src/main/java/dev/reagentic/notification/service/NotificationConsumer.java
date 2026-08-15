package dev.reagentic.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reagentic.common.events.PaymentCompletedEvent;
import dev.reagentic.common.events.PaymentFailedEvent;
import dev.reagentic.notification.domain.NotificationRecord;
import dev.reagentic.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private static final String TOPIC = "payment-events";

    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(NotificationRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = TOPIC, groupId = "notification-service")
    public void consume(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.path("eventType").asText();
            switch (eventType) {
                case "PaymentCompleted" -> handle(
                        objectMapper.treeToValue(node, PaymentCompletedEvent.class), "PAYMENT_COMPLETED");
                case "PaymentFailed" -> handleFailed(
                        objectMapper.treeToValue(node, PaymentFailedEvent.class), "PAYMENT_FAILED");
                default -> log.warn("Notification ignoring unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Notification failed to process event payload: {}", e.getMessage());
        }
    }

    @Transactional
    public void handle(PaymentCompletedEvent e, String type) {
        if (repository.existsByPaymentId(e.paymentId())) {
            return;
        }
        repository.save(new NotificationRecord(e.paymentId(), type, e.sourceAccountId(),
                e.amount().asString(), e.currency(), "SENT"));
        log.info("NOTIFICATION [{}] payment={} amount={} {} source={}",
                type, e.paymentId(), e.amount().asString(), e.currency(), e.sourceAccountId());
    }

    @Transactional
    public void handleFailed(PaymentFailedEvent e, String type) {
        if (repository.existsByPaymentId(e.paymentId())) {
            return;
        }
        repository.save(new NotificationRecord(e.paymentId(), type, e.sourceAccountId(),
                e.amount().asString(), e.currency(), "SENT"));
        log.info("NOTIFICATION [{}] payment={} amount={} {} source={} reason pending",
                type, e.paymentId(), e.amount().asString(), e.currency(), e.sourceAccountId());
    }
}
