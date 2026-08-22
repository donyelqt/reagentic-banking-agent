package dev.reagentic.transaction.controller;

import dev.reagentic.common.dto.ApiResponse;
import dev.reagentic.transaction.model.Transaction;
import dev.reagentic.transaction.model.dto.TransactionUploadResponse;
import dev.reagentic.transaction.repository.TransactionRepository;
import dev.reagentic.transaction.service.TransactionUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionUploadService uploadService;
    private final TransactionRepository transactionRepository;

    public TransactionController(TransactionUploadService uploadService, TransactionRepository transactionRepository) {
        this.uploadService = uploadService;
        this.transactionRepository = transactionRepository;
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<TransactionUploadResponse>> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "accountId", required = false) String accountIdParam,
            @RequestHeader(value = "X-Account-Id", required = false) String headerAccountId,
            Authentication authentication) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("File is empty or missing"));
        }

        String accountId = accountIdParam != null && !accountIdParam.isBlank()
                ? accountIdParam
                : (headerAccountId != null && !headerAccountId.isBlank() ? headerAccountId : "acc-checking-0001");

        TransactionUploadResponse response = uploadService.processUpload(file, accountId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ApiResponse<List<Transaction>> listTransactions(
            @RequestParam(value = "accountId", required = false) String accountIdParam,
            @RequestHeader(value = "X-Account-Id", required = false) String headerAccountId) {
        
        String accountId = accountIdParam != null && !accountIdParam.isBlank()
                ? accountIdParam
                : (headerAccountId != null && !headerAccountId.isBlank() ? headerAccountId : "acc-checking-0001");

        return ApiResponse.ok(transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId));
    }

    @GetMapping("/batch/{uploadBatchId}")
    public ApiResponse<List<Transaction>> listByBatch(@PathVariable String uploadBatchId) {
        return ApiResponse.ok(transactionRepository.findByUploadBatchId(uploadBatchId));
    }

    @DeleteMapping("/batch/{uploadBatchId}")
    @Transactional
    public ApiResponse<String> deleteBatch(@PathVariable String uploadBatchId) {
        transactionRepository.deleteByUploadBatchId(uploadBatchId);
        return ApiResponse.ok("Batch deleted successfully");
    }

    @GetMapping("/summary")
    public ApiResponse<List<TransactionRepository.CategoryTotal>> getSummary(
            @RequestParam(value = "accountId", required = false, defaultValue = "acc-checking-0001") String accountId) {
        return ApiResponse.ok(transactionRepository.sumAmountByCategoryForAccount(accountId));
    }
}
