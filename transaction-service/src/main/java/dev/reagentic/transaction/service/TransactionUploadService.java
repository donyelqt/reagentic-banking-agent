package dev.reagentic.transaction.service;

import dev.reagentic.transaction.model.Transaction;
import dev.reagentic.transaction.model.dto.TransactionUploadResponse;
import dev.reagentic.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionUploadService {

    private final CsvParsingService csvParsingService;
    private final TransactionCategorizationService categorizationService;
    private final TransactionRepository transactionRepository;
    private final TransactionEventPublisher eventPublisher;

    public TransactionUploadService(CsvParsingService csvParsingService,
                                   TransactionCategorizationService categorizationService,
                                   TransactionRepository transactionRepository,
                                   TransactionEventPublisher eventPublisher) {
        this.csvParsingService = csvParsingService;
        this.categorizationService = categorizationService;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TransactionUploadResponse processUpload(MultipartFile file, String accountId) {
        String uploadBatchId = UUID.randomUUID().toString();

        CsvParsingService.ParseResult parseResult = csvParsingService.parse(file, accountId, uploadBatchId);
        List<Transaction> transactions = parseResult.getTransactions();

        categorizationService.categorizeAll(transactions);

        List<Transaction> saved = transactionRepository.saveAll(transactions);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishImportCompleted(accountId, saved, parseResult.getErrors().size());
                }
            });
        } else {
            eventPublisher.publishImportCompleted(accountId, saved, parseResult.getErrors().size());
        }

        return TransactionUploadResponse.builder()
                .uploadBatchId(uploadBatchId)
                .rowsParsed(saved.size())
                .rowsRejected(parseResult.getErrors().size())
                .rejectedRowErrors(parseResult.getErrors())
                .totalsByCategory(totalsByCategory(saved))
                .netTotal(saved.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .build();
    }

    private Map<String, BigDecimal> totalsByCategory(List<Transaction> transactions) {
        return transactions.stream().collect(Collectors.groupingBy(
                t -> t.getCategory().name(),
                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));
    }
}
