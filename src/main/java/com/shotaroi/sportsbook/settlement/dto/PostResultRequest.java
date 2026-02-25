package com.shotaroi.sportsbook.settlement.dto;

import com.shotaroi.sportsbook.common.domain.Selection;

/**
 * Winning selection: HOME, DRAW, AWAY. Null = VOID (refund all stakes).
 */
public record PostResultRequest(
        Selection winningSelection  // null = VOID
) {}
