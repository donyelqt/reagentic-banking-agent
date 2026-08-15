package dev.reagentic.ledger.repository;

import dev.reagentic.ledger.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    Optional<LedgerEntry> findTopByAccountIdOrderByCreatedAtDesc(String accountId);

    boolean existsByPaymentId(String paymentId);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtAsc(String accountId);

    @Query("SELECT COALESCE(SUM(e.signedAmount), 0) FROM LedgerEntry e WHERE e.accountId = :acc")
    BigDecimal sumSigned(@Param("acc") String accountId);
}
