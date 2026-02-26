package com.shotaroi.sportsbook.risk.service;

import com.shotaroi.sportsbook.AbstractIntegrationTest;
import com.shotaroi.sportsbook.common.domain.MarketType;
import com.shotaroi.sportsbook.common.domain.RiskDecision;
import com.shotaroi.sportsbook.common.domain.Selection;
import com.shotaroi.sportsbook.risk.dto.RiskDecisionResult;
import com.shotaroi.sportsbook.risk.entity.Limit;
import com.shotaroi.sportsbook.risk.repository.ExposureRepository;
import com.shotaroi.sportsbook.risk.repository.LimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RiskEngineServiceTest extends AbstractIntegrationTest {

    @Autowired
    RiskEngineService riskEngineService;
    @Autowired
    ExposureRepository exposureRepository;
    @Autowired
    LimitRepository limitRepository;

    private static final String EVENT_ID = "evt-1";

    @BeforeEach
    void setUp() {
        limitRepository.deleteAll();
        exposureRepository.deleteAll();
    }

    @Test
    void evaluate_acceptsWhenNoLimits() {
        RiskDecisionResult result = riskEngineService.evaluate(
                EVENT_ID, MarketType.MATCH_WINNER, Selection.HOME,
                new BigDecimal("100"), new BigDecimal("85")  // stake 100, liability 85
        );
        assertEquals(RiskDecision.ACCEPT, result.decision());
        assertTrue(result.maxAllowedStake().compareTo(new BigDecimal("100")) == 0);
    }

    @Test
    void evaluate_acceptWithLimitWhenStakeExceedsMaxPerBet() {
        Limit limit = new Limit();
        limit.setScopeType("EVENT");
        limit.setScopeId(EVENT_ID);
        limit.setMaxStakePerBet(new BigDecimal("50"));
        limitRepository.save(limit);

        RiskDecisionResult result = riskEngineService.evaluate(
                EVENT_ID, MarketType.MATCH_WINNER, Selection.HOME,
                new BigDecimal("100"), new BigDecimal("85")
        );
        assertEquals(RiskDecision.ACCEPT_WITH_LIMIT, result.decision());
        assertEquals(new BigDecimal("50.00"), result.maxAllowedStake());
    }

    @Test
    void evaluate_rejectsWhenLiabilityLimitReached() {
        Limit limit2 = new Limit();
        limit2.setScopeType("EVENT_MARKET_SELECTION");
        limit2.setScopeId(EVENT_ID + "|MATCH_WINNER|HOME");
        limit2.setMaxReservedLiability(new BigDecimal("50"));
        limitRepository.save(limit2);

        // Create exposure with 50 already reserved
        var exp = new com.shotaroi.sportsbook.risk.entity.Exposure();
        exp.setEventId(EVENT_ID);
        exp.setMarketType(MarketType.MATCH_WINNER);
        exp.setSelection(Selection.HOME);
        exp.setReservedLiability(new BigDecimal("50"));
        exp.setVersion(0L);
        exposureRepository.save(exp);

        RiskDecisionResult result = riskEngineService.evaluate(
                EVENT_ID, MarketType.MATCH_WINNER, Selection.HOME,
                new BigDecimal("100"), new BigDecimal("85")
        );
        assertEquals(RiskDecision.REJECT, result.decision());
    }
}
