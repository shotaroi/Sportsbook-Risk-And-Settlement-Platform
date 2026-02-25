package com.shotaroi.sportsbook.ledger.service;

import com.shotaroi.sportsbook.common.domain.LedgerEntryType;
import com.shotaroi.sportsbook.common.domain.LedgerReferenceType;
import com.shotaroi.sportsbook.common.error.InsufficientFundsException;
import com.shotaroi.sportsbook.common.util.MoneyUtil;
import com.shotaroi.sportsbook.ledger.entity.LedgerEntry;
import com.shotaroi.sportsbook.ledger.repository.LedgerEntryRepository;
import com.shotaroi.sportsbook.ledger.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Append-only ledger. Balance is derived from sum of entries, never mutated directly.
 */
@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);
    private static final String CURRENCY = "SEK";

    private final LedgerEntryRepository ledgerRepository;
    private final WalletRepository walletRepository;

    public LedgerService(LedgerEntryRepository ledgerRepository, WalletRepository walletRepository) {
        this.ledgerRepository = ledgerRepository;
        this.walletRepository = walletRepository;
    }

    /**
     * Debit stake from customer wallet. Fails if insufficient balance.
     */
    @Transactional
    public void debitStake(Long customerId, BigDecimal amount, String referenceId) {
        MoneyUtil.validatePositiveStake(amount);
        BigDecimal balance = getBalance(customerId);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(customerId, amount, balance);
        }
        ensureWalletExists(customerId);
        LedgerEntry entry = new LedgerEntry();
        entry.setCustomerId(customerId);
        entry.setType(LedgerEntryType.DEBIT);
        entry.setAmount(MoneyUtil.money(amount));
        entry.setCurrency(CURRENCY);
        entry.setReferenceType(LedgerReferenceType.BET_STAKE);
        entry.setReferenceId(referenceId);
        ledgerRepository.save(entry);
        log.info("Ledger debit: customerId={}, amount={}, reference={}", customerId, amount, referenceId);
    }

    /**
     * Credit payout to customer (bet won).
     */
    @Transactional
    public void creditPayout(Long customerId, BigDecimal amount, String referenceId) {
        MoneyUtil.validatePositiveStake(amount);
        ensureWalletExists(customerId);
        LedgerEntry entry = new LedgerEntry();
        entry.setCustomerId(customerId);
        entry.setType(LedgerEntryType.CREDIT);
        entry.setAmount(MoneyUtil.money(amount));
        entry.setCurrency(CURRENCY);
        entry.setReferenceType(LedgerReferenceType.BET_PAYOUT);
        entry.setReferenceId(referenceId);
        ledgerRepository.save(entry);
        log.info("Ledger credit: customerId={}, amount={}, reference={}", customerId, amount, referenceId);
    }

    /**
     * Refund stake (bet void).
     */
    @Transactional
    public void refundStake(Long customerId, BigDecimal amount, String referenceId) {
        MoneyUtil.validatePositiveStake(amount);
        ensureWalletExists(customerId);
        LedgerEntry entry = new LedgerEntry();
        entry.setCustomerId(customerId);
        entry.setType(LedgerEntryType.REFUND);
        entry.setAmount(MoneyUtil.money(amount));
        entry.setCurrency(CURRENCY);
        entry.setReferenceType(LedgerReferenceType.BET_REFUND);
        entry.setReferenceId(referenceId);
        ledgerRepository.save(entry);
        log.info("Ledger refund: customerId={}, amount={}, reference={}", customerId, amount, referenceId);
    }

    /** Derive balance from ledger entries. */
    public BigDecimal getBalance(Long customerId) {
        BigDecimal sum = ledgerRepository.sumBalanceByCustomerId(customerId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private void ensureWalletExists(Long customerId) {
        walletRepository.findByCustomerIdAndCurrency(customerId, CURRENCY)
                .orElseGet(() -> {
                    var wallet = new com.shotaroi.sportsbook.ledger.entity.Wallet();
                    wallet.setCustomerId(customerId);
                    wallet.setCurrency(CURRENCY);
                    return walletRepository.save(wallet);
                });
    }
}
