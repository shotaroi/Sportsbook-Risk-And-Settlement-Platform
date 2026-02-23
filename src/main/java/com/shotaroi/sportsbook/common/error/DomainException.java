package com.shotaroi.sportsbook.common.error;

/**
 * Base for domain-level exceptions that map to HTTP 4xx with ProblemDetails.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
