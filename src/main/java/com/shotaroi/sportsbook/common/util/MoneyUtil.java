package com.shotaroi.sportsbook.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money and odds utilities. All money in SEK with scale 2.
 * Never use float/double for money.
 */
public final class MoneyUtil {

    public static final int MONEY_SCALE = 2;
    public static final int ODDS_SCALE = 3;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private MoneyUtil() {
    }

    /** Normalize money to scale 2 (SEK). */
    public static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(MONEY_SCALE, ROUNDING);
    }

    /** Normalize odds to scale 3. */
    public static BigDecimal odds(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(ODDS_SCALE, ROUNDING);
    }

    /** Calculate potential payout: stake * odds. */
    public static BigDecimal potentialPayout(BigDecimal stake, BigDecimal odds) {
        return money(stake.multiply(odds));
    }

    /** Validate stake is positive. */
    public static void validatePositiveStake(BigDecimal stake) {
        if (stake == null || stake.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stake must be positive");
        }
    }

    /** Validate odds are valid (>= 1.001). */
    public static void validateOdds(BigDecimal odds) {
        if (odds == null || odds.compareTo(new BigDecimal("1.001")) < 0) {
            throw new IllegalArgumentException("Odds must be >= 1.001");
        }
    }
}
