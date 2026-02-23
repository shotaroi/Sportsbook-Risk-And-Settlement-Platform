package com.shotaroi.sportsbook.common.error;

import java.math.BigDecimal;

public class RiskLimitExceededException extends DomainException {

    private final BigDecimal requestedStake;
    private final BigDecimal maxAllowedStake;

    public RiskLimitExceededException(BigDecimal requestedStake, BigDecimal maxAllowedStake) {
        super("Risk limit exceeded: requested stake %s exceeds max allowed %s"
                .formatted(requestedStake, maxAllowedStake));
        this.requestedStake = requestedStake;
        this.maxAllowedStake = maxAllowedStake;
    }

    public BigDecimal getRequestedStake() {
        return requestedStake;
    }

    public BigDecimal getMaxAllowedStake() {
        return maxAllowedStake;
    }
}
