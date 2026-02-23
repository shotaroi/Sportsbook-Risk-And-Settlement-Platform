package com.shotaroi.sportsbook.common.domain;

/**
 * Risk engine decision for bet placement.
 */
public enum RiskDecision {
    /** Bet accepted at full requested stake */
    ACCEPT,
    /** Bet accepted but stake reduced to maxAllowedStake */
    ACCEPT_WITH_LIMIT,
    /** Bet rejected (limit exceeded, market suspended, etc.) */
    REJECT
}
