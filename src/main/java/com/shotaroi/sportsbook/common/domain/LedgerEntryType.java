package com.shotaroi.sportsbook.common.domain;

/**
 * Ledger entry types for audit and balance derivation.
 */
public enum LedgerEntryType {
    DEBIT,   // Stake deducted
    CREDIT,  // Payout credited
    REFUND   // Void refund
}
