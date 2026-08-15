package dev.reagentic.account.service;

import dev.reagentic.account.domain.Account;
import dev.reagentic.account.domain.AccountIdempotency;
import dev.reagentic.account.repository.AccountIdempotencyRepository;
import dev.reagentic.account.repository.AccountRepository;
import dev.reagentic.common.money.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountIdempotencyRepository idemRepository;

    public AccountService(AccountRepository accountRepository, AccountIdempotencyRepository idemRepository) {
        this.accountRepository = accountRepository;
        this.idemRepository = idemRepository;
    }

    public List<Account> listForUser(String email) {
        return accountRepository.findByUserId(email);
    }

    public Account getForUser(String email, String accountId) {
        return accountRepository.findByAccountIdAndUserId(accountId, email)
                .orElseThrow(() -> new AccountException("ACCOUNT_NOT_FOUND", "Account not found or not owned by caller"));
    }

    @Transactional
    public Money debit(String email, String accountId, Money amount, String idempotencyKey) {
        Account account = loadOwned(email, accountId);
        Optional<AccountIdempotency> existing = idemRepository.findByKey(idempotencyKey);
        if (existing.isPresent()) {
            return Money.of(account.getBalance());
        }
        if (account.getBalance().compareTo(amount.value()) < 0) {
            throw new InsufficientFundsException();
        }
        account.setBalance(account.getBalance().subtract(amount.value()));
        accountRepository.save(account);
        idemRepository.save(new AccountIdempotency(idempotencyKey, accountId, "DEBIT", amount.value()));
        return Money.of(account.getBalance());
    }

    @Transactional
    public Money credit(String email, String accountId, Money amount, String idempotencyKey) {
        Account account = loadOwned(email, accountId);
        Optional<AccountIdempotency> existing = idemRepository.findByKey(idempotencyKey);
        if (existing.isPresent()) {
            return Money.of(account.getBalance());
        }
        account.setBalance(account.getBalance().add(amount.value()));
        accountRepository.save(account);
        idemRepository.save(new AccountIdempotency(idempotencyKey, accountId, "CREDIT", amount.value()));
        return Money.of(account.getBalance());
    }

    private Account loadOwned(String email, String accountId) {
        return accountRepository.findByAccountIdAndUserId(accountId, email)
                .orElseThrow(() -> new AccountException("ACCOUNT_NOT_FOUND", "Account not found or not owned by caller"));
    }

    public static class AccountException extends RuntimeException {
        private final String code;

        public AccountException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException() {
            super("Insufficient funds");
        }
    }
}
