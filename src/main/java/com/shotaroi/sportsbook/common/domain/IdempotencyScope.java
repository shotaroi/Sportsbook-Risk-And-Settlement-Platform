package com.shotaroi.sportsbook.common.domain;

/**
 * Idempotency key scopes.
 */
public enum IdempotencyScope {
    BET_PLACEMENT,
    RESULT_INGEST
}
