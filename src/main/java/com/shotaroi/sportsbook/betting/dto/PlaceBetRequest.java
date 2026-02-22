package com.shotaroi.sportsbook.betting.dto;

import com.shotaroi.sportsbook.common.domain.MarketType;
import com.shotaroi.sportsbook.common.domain.Selection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PlaceBetRequest(
        @NotNull Long customerId,
        @NotNull String eventId,
        @NotNull MarketType marketType,
        @NotNull Selection selection,
        @NotNull @DecimalMin("1.001") BigDecimal odds,
        @NotNull @Positive BigDecimal stake
) {}
