package com.shotaroi.sportsbook.risk.dto;

import com.shotaroi.sportsbook.common.domain.RiskDecision;

import java.math.BigDecimal;

public record RiskDecisionResult(
        RiskDecision decision,
        BigDecimal maxAllowedStake,
        String rejectReason
) {
    public static RiskDecisionResult accept(BigDecimal stake) {
        return new RiskDecisionResult(
                com.shotaroi.sportsbook.common.domain.RiskDecision.ACCEPT,
                stake,
                null
        );
    }

    public static RiskDecisionResult acceptWithLimit(BigDecimal maxAllowedStake) {
        return new RiskDecisionResult(
                com.shotaroi.sportsbook.common.domain.RiskDecision.ACCEPT_WITH_LIMIT,
                maxAllowedStake,
                null
        );
    }

    public static RiskDecisionResult reject(String reason) {
        return new RiskDecisionResult(
                com.shotaroi.sportsbook.common.domain.RiskDecision.REJECT,
                BigDecimal.ZERO,
                reason
        );
    }
}
