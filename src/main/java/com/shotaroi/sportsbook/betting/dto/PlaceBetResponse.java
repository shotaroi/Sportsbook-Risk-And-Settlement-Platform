package com.shotaroi.sportsbook.betting.dto;

import com.shotaroi.sportsbook.common.domain.BetStatus;
import com.shotaroi.sportsbook.common.domain.RiskDecision;

import java.math.BigDecimal;

public record PlaceBetResponse(
        Long betId,
        BetStatus status,
        BigDecimal acceptedStake,
        BigDecimal potentialPayout,
        RiskDecision decision
) {}
