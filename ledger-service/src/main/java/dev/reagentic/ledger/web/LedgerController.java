package dev.reagentic.ledger.web;

import dev.reagentic.common.dto.ApiResponse;
import dev.reagentic.ledger.domain.LedgerEntry;
import dev.reagentic.ledger.repository.LedgerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerRepository repository;

    public LedgerController(LedgerRepository repository) {
        this.repository = repository;
    }

    public record LedgerView(Long entryId, String accountId, String paymentId, String type,
                             String signedAmount, String balanceAfter, long createdAt) {
    }

    @GetMapping("/{accountId}")
    public ApiResponse<List<LedgerView>> list(@PathVariable String accountId,
                                              @RequestParam(required = false) Long from,
                                              @RequestParam(required = false) Long to) {
        List<LedgerEntry> entries = repository.findByAccountIdOrderByCreatedAtAsc(accountId);
        if (from != null) {
            entries = entries.stream().filter(e -> e.getCreatedAt() >= from).toList();
        }
        if (to != null) {
            entries = entries.stream().filter(e -> e.getCreatedAt() <= to).toList();
        }
        List<LedgerView> views = entries.stream().map(e -> new LedgerView(
                e.getEntryId(), e.getAccountId(), e.getPaymentId(), e.getType(),
                e.getSignedAmount().toPlainString(), e.getBalanceAfter().toPlainString(), e.getCreatedAt()))
                .toList();
        return ApiResponse.ok(views);
    }
}
