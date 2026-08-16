package dev.reagentic.ledger.web;

import dev.reagentic.common.dto.ApiResponse;
import dev.reagentic.ledger.domain.LedgerEntry;
import dev.reagentic.ledger.repository.LedgerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerRepository repository;
    private final RestClient accountClient;

    public LedgerController(LedgerRepository repository, RestClient accountClient) {
        this.repository = repository;
        this.accountClient = accountClient;
    }

    public record LedgerView(Long entryId, String accountId, String paymentId, String type,
                             String signedAmount, String balanceAfter, long createdAt) {
    }

    @GetMapping("/{accountId}")
    public ApiResponse<List<LedgerView>> list(HttpServletRequest request, @PathVariable String accountId,
                                              @RequestParam(required = false) Long from,
                                              @RequestParam(required = false) Long to) {
        verifyOwnership(request.getHeader("Authorization"), accountId);
        return ApiResponse.ok(toViews(repository.findByAccountIdOrderByCreatedAtAsc(accountId), from, to));
    }

    /**
     * The ledger is append-only and holds no ownership data, so ownership is
     * delegated to account-service: the caller must be able to read the account
     * through its ownership-scoped endpoint. A USER probing another customer's
     * account id gets the same 404 as if the account did not exist.
     */
    private void verifyOwnership(String authHeader, String accountId) {
        try {
            accountClient.get()
                    .uri("/api/accounts/{accountId}/balance", accountId)
                    .header("Authorization", authHeader)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw new NotOwnedException();
        }
    }

    @GetMapping("/internal/{accountId}")
    public ApiResponse<List<LedgerView>> listInternal(Authentication auth, @PathVariable String accountId,
                                                      @RequestParam(required = false) Long from,
                                                      @RequestParam(required = false) Long to) {
        if (!isEmployee(auth)) {
            throw new AccessDeniedException("EMPLOYEE role required");
        }
        return ApiResponse.ok(toViews(repository.findByAccountIdOrderByCreatedAtAsc(accountId), from, to));
    }

    private List<LedgerView> toViews(List<LedgerEntry> entries, Long from, Long to) {
        if (from != null) {
            entries = entries.stream().filter(e -> e.getCreatedAt() >= from).toList();
        }
        if (to != null) {
            entries = entries.stream().filter(e -> e.getCreatedAt() <= to).toList();
        }
        return entries.stream().map(e -> new LedgerView(
                e.getEntryId(), e.getAccountId(), e.getPaymentId(), e.getType(),
                e.getSignedAmount().toPlainString(), e.getBalanceAfter().toPlainString(), e.getCreatedAt()))
                .toList();
    }

    private boolean isEmployee(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_EMPLOYEE".equals(a.getAuthority()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("status", 403, "code", "FORBIDDEN", "message", "EMPLOYEE role required"));
    }

    @ExceptionHandler(NotOwnedException.class)
    public ResponseEntity<Map<String, Object>> notOwned() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", 404, "code", "ACCOUNT_NOT_FOUND",
                        "message", "Account not found or not owned by caller"));
    }

    public static class NotOwnedException extends RuntimeException {
    }
}
