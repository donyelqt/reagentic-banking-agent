package dev.reagentic.account.repository;

import dev.reagentic.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByAccountIdAndUserId(String accountId, String userId);
    Optional<Account> findByAccountId(String accountId);
    List<Account> findByUserId(String userId);
}
