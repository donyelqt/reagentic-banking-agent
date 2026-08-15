package dev.reagentic.account.repository;

import dev.reagentic.account.domain.AccountIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountIdempotencyRepository extends JpaRepository<AccountIdempotency, Long> {
    Optional<AccountIdempotency> findByKey(String key);
}
