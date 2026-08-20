package dev.reagentic.ledger.repository;

import dev.reagentic.ledger.domain.LedgerEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    Optional<LedgerEntry> findTopByAccountIdOrderByEntryIdDesc(String accountId);

    /**
     * Same last-entry read as {@link #findTopByAccountIdOrderByEntryIdDesc} but
     * with a pessimistic write lock so concurrent appends on one account chain
     * balance_after serially instead of both reading the same predecessor.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from LedgerEntry e where e.accountId = :accountId order by e.entryId desc")
    Optional<LedgerEntry> findTopByAccountIdOrderByEntryIdDescForUpdate(@Param("accountId") String accountId);

    boolean existsByPaymentId(String paymentId);

    List<LedgerEntry> findByAccountIdOrderByCreatedAtAsc(String accountId);

    @Query("SELECT COALESCE(SUM(e.signedAmount), 0) FROM LedgerEntry e WHERE e.accountId = :acc")
    BigDecimal sumSigned(@Param("acc") String accountId);
}
