package com.shotaroi.sportsbook.common.error;

/**
 * Thrown when idempotency key matches but request body differs (replay attack or client error).
 */
public class DuplicateIdempotencyKeyException extends DomainException {

    private final String idempotencyKey;
    private final String scope;

    public DuplicateIdempotencyKeyException(String idempotencyKey, String scope) {
        super("Duplicate idempotency key '%s' for scope '%s' with different request body"
                .formatted(idempotencyKey, scope));
        this.idempotencyKey = idempotencyKey;
        this.scope = scope;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getScope() {
        return scope;
    }
}
