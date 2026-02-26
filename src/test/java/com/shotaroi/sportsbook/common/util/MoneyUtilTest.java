package com.shotaroi.sportsbook.common.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyUtilTest {

    @Test
    void potentialPayout() {
        assertEquals(new BigDecimal("185.00"), MoneyUtil.potentialPayout(new BigDecimal("100"), new BigDecimal("1.85")));
        assertEquals(new BigDecimal("200.00"), MoneyUtil.potentialPayout(new BigDecimal("100"), new BigDecimal("2")));
    }

    @Test
    void validatePositiveStake_throwsWhenZero() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.validatePositiveStake(BigDecimal.ZERO));
    }

    @Test
    void validatePositiveStake_throwsWhenNegative() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.validatePositiveStake(new BigDecimal("-10")));
    }

    @Test
    void validateOdds_throwsWhenTooLow() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.validateOdds(new BigDecimal("1")));
    }
}
