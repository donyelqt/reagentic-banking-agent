package dev.reagentic.account.web;

import dev.reagentic.account.service.AccountService;
import dev.reagentic.common.dto.ApiResponse;
import dev.reagentic.common.money.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    public record AccountView(String accountId, String type, Money balance) {
    }

    public record BalanceView(String accountId, Money balance) {
    }

    public record MutateRequest(@NotBlank String accountId, @NotBlank String amount, @NotBlank String idempotencyKey) {
    }

    @GetMapping
    public ApiResponse<List<AccountView>> list(Authentication auth) {
        String email = auth.getName();
        List<AccountView> views = accountService.listForUser(email).stream()
                .map(a -> new AccountView(a.getAccountId(), a.getType(), Money.of(a.getBalance())))
                .toList();
        return ApiResponse.ok(views);
    }

    @GetMapping("/{accountId}/balance")
    public ApiResponse<BalanceView> balance(Authentication auth, @PathVariable String accountId) {
        var a = accountService.getForUser(auth.getName(), accountId);
        return ApiResponse.ok(new BalanceView(a.getAccountId(), Money.of(a.getBalance())));
    }

    @GetMapping("/internal/balance/{accountId}")
    public ApiResponse<BalanceView> balanceInternal(Authentication auth, @PathVariable String accountId) {
        if (!isEmployee(auth)) {
            throw new AccessDeniedException("EMPLOYEE role required");
        }
        var a = accountService.getAny(accountId);
        return ApiResponse.ok(new BalanceView(a.getAccountId(), Money.of(a.getBalance())));
    }

    @PostMapping("/internal/debit")
    public ApiResponse<BalanceView> debit(Authentication auth, @Valid @RequestBody MutateRequest req) {
        Money balance = accountService.debit(auth.getName(), req.accountId(), Money.of(req.amount()), req.idempotencyKey());
        return ApiResponse.ok(new BalanceView(req.accountId(), balance));
    }

    @PostMapping("/internal/credit")
    public ApiResponse<BalanceView> credit(Authentication auth, @Valid @RequestBody MutateRequest req) {
        Money balance = accountService.credit(auth.getName(), req.accountId(), Money.of(req.amount()), req.idempotencyKey());
        return ApiResponse.ok(new BalanceView(req.accountId(), balance));
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

    @ExceptionHandler(AccountService.InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> insufficient() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("status", 409, "code", "INSUFFICIENT_FUNDS", "message", "Insufficient funds"));
    }
}
