package com.shotaroi.sportsbook.ledger.repository;

import com.shotaroi.sportsbook.ledger.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Page<LedgerEntry> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    /**
     * Derive balance: sum of credits minus debits for customer.
     * DEBIT = negative, CREDIT/REFUND = positive.
     */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN e.type = 'DEBIT' THEN -e.amount ELSE e.amount END), 0)
            FROM LedgerEntry e WHERE e.customerId = :customerId
            """)
    BigDecimal sumBalanceByCustomerId(Long customerId);
}
