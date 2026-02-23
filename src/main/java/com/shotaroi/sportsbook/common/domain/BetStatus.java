package com.shotaroi.sportsbook.common.domain;

/**
 * Bet lifecycle status.
 */
public enum BetStatus {
    PLACED,
    SETTLED_WON,
    SETTLED_LOST,
    SETTLED_VOID
}
