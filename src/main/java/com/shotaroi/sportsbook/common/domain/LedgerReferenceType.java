package com.shotaroi.sportsbook.common.domain;

/**
 * Reference type for ledger entries (links to bet, settlement, etc.).
 */
public enum LedgerReferenceType {
    BET_STAKE,
    BET_PAYOUT,
    BET_REFUND
}
