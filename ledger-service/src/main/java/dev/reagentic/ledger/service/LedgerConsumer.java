package dev.reagentic.ledger.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.reagentic.common.events.PaymentCompletedEvent;
import dev.reagentic.common.events.PaymentFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LedgerConsumer {

    private static final Logger log = LoggerFactory.getLogger(LedgerConsumer.class);
    private static final String TOPIC = "payment-events";

    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;

    public LedgerConsumer(LedgerService ledgerService, ObjectMapper objectMapper) {
        this.ledgerService = ledgerService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = TOPIC, groupId = "ledger-service")
    public void consume(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.path("eventType").asText();
            switch (eventType) {
                case "PaymentCompleted" -> ledgerService.applyCompleted(
                        objectMapper.treeToValue(node, PaymentCompletedEvent.class));
                case "PaymentFailed" -> ledgerService.applyFailed(
                        objectMapper.treeToValue(node, PaymentFailedEvent.class));
                default -> log.warn("Ledger ignoring unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Ledger failed to process event payload: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
