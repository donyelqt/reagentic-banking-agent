package dev.reagentic.transaction.service;

import dev.reagentic.transaction.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishImportCompleted(String accountId, List<Transaction> transactions, int rejectedRows) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", "TRANSACTION_IMPORT_COMPLETED");
            event.put("accountId", accountId);
            event.put("importedCount", transactions.size());
            event.put("rejectedCount", rejectedRows);

            kafkaTemplate.send("transaction-events", accountId, event);
            log.info("Published TRANSACTION_IMPORT_COMPLETED event for accountId={}, count={}", accountId, transactions.size());
        } catch (Exception e) {
            log.warn("Failed to publish Kafka event for transaction import (non-fatal): {}", e.getMessage());
        }
    }
}
