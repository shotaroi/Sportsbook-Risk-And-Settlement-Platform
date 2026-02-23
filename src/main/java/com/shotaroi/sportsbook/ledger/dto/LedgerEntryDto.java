package com.shotaroi.sportsbook.ledger.dto;

import com.shotaroi.sportsbook.common.domain.LedgerEntryType;
import com.shotaroi.sportsbook.common.domain.LedgerReferenceType;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryDto(
        Long id,
        LedgerEntryType type,
        BigDecimal amount,
        String currency,
        LedgerReferenceType referenceType,
        String referenceId,
        Instant createdAt
) {}
