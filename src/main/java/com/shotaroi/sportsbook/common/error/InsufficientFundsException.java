package com.shotaroi.sportsbook.common.error;

import java.math.BigDecimal;

public class InsufficientFundsException extends DomainException {

    private final Long customerId;
    private final BigDecimal required;
    private final BigDecimal available;

    public InsufficientFundsException(Long customerId, BigDecimal required, BigDecimal available) {
        super("Insufficient funds: customer %d has %s SEK, required %s SEK"
                .formatted(customerId, available, required));
        this.customerId = customerId;
        this.required = required;
        this.available = available;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public BigDecimal getRequired() {
        return required;
    }

    public BigDecimal getAvailable() {
        return available;
    }
}
