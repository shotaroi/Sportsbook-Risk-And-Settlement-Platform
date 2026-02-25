package com.shotaroi.sportsbook.risk.dto;

import com.shotaroi.sportsbook.common.domain.MarketType;
import com.shotaroi.sportsbook.common.domain.Selection;

import java.math.BigDecimal;

public record ExposureDto(
        Long id,
        String eventId,
        MarketType marketType,
        Selection selection,
        BigDecimal reservedLiability
) {}
