package com.shotaroi.sportsbook.risk.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RiskLimitRequest(
        @NotNull String scopeType,   // GLOBAL, EVENT, EVENT_MARKET_SELECTION
        String scopeId,           // eventId or eventId|marketType|selection
        BigDecimal maxReservedLiability,
        BigDecimal maxStakePerBet
) {}
