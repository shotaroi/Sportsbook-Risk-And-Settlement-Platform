package com.shotaroi.sportsbook.risk.service;

import com.shotaroi.sportsbook.common.domain.MarketType;
import com.shotaroi.sportsbook.common.domain.RiskDecision;
import com.shotaroi.sportsbook.common.domain.Selection;
import com.shotaroi.sportsbook.common.util.MoneyUtil;
import com.shotaroi.sportsbook.risk.dto.RiskDecisionResult;
import com.shotaroi.sportsbook.risk.entity.Exposure;
import com.shotaroi.sportsbook.risk.entity.Limit;
import com.shotaroi.sportsbook.risk.repository.ExposureRepository;
import com.shotaroi.sportsbook.risk.repository.LimitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Risk engine: evaluates limits and returns ACCEPT / ACCEPT_WITH_LIMIT / REJECT.
 * Does NOT reserve exposure; that is done by RiskReservationService.
 */
@Service
public class RiskEngineService {

    private static final Logger log = LoggerFactory.getLogger(RiskEngineService.class);

    private static final String SCOPE_GLOBAL = "GLOBAL";
    private static final String SCOPE_EVENT = "EVENT";
    private static final String SCOPE_EVENT_MARKET_SELECTION = "EVENT_MARKET_SELECTION";

    private final ExposureRepository exposureRepository;
    private final LimitRepository limitRepository;

    public RiskEngineService(ExposureRepository exposureRepository, LimitRepository limitRepository) {
        this.exposureRepository = exposureRepository;
        this.limitRepository = limitRepository;
    }

    /**
     * Evaluate risk for a bet. Returns decision and maxAllowedStake.
     */
    public RiskDecisionResult evaluate(String eventId, MarketType marketType, Selection selection,
                                       BigDecimal requestedStake, BigDecimal potentialLiability) {
        MoneyUtil.validatePositiveStake(requestedStake);

        // 1. Check per-bet stake limit
        BigDecimal maxStakePerBet = getMaxStakePerBet(eventId, marketType, selection);
        if (requestedStake.compareTo(maxStakePerBet) > 0) {
            if (maxStakePerBet.compareTo(BigDecimal.ZERO) <= 0) {
                return RiskDecisionResult.reject("Stake limit exceeded: no bets allowed");
            }
            return RiskDecisionResult.acceptWithLimit(maxStakePerBet);
        }

        // 2. Check liability limits
        BigDecimal currentLiability = getCurrentReservedLiability(eventId, marketType, selection);
        BigDecimal maxLiability = getMaxReservedLiability(eventId, marketType, selection);

        if (maxLiability != null && maxLiability.compareTo(BigDecimal.ZERO) >= 0) {
            BigDecimal newLiability = currentLiability.add(potentialLiability);
            if (newLiability.compareTo(maxLiability) > 0) {
                BigDecimal remaining = maxLiability.subtract(currentLiability);
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    return RiskDecisionResult.reject("Liability limit reached for selection");
                }
                // Liability = stake * (odds - 1), so stake = liability / (odds - 1)
                // potentialLiability = requestedStake * (odds - 1) => odds - 1 = potentialLiability / requestedStake
                BigDecimal oddsMinusOne = potentialLiability.divide(requestedStake, 6, java.math.RoundingMode.HALF_UP);
                if (oddsMinusOne.compareTo(BigDecimal.ZERO) <= 0) {
                    return RiskDecisionResult.reject("Invalid odds for liability calculation");
                }
                BigDecimal maxStakeFromRemaining = remaining.divide(oddsMinusOne, 2, java.math.RoundingMode.DOWN);
                BigDecimal effectiveMax = maxStakeFromRemaining.min(maxStakePerBet);
                if (effectiveMax.compareTo(BigDecimal.ZERO) <= 0) {
                    return RiskDecisionResult.reject("Liability limit reached");
                }
                if (requestedStake.compareTo(effectiveMax) > 0) {
                    return RiskDecisionResult.acceptWithLimit(effectiveMax);
                }
            }
        }

        return RiskDecisionResult.accept(requestedStake);
    }

    private BigDecimal getCurrentReservedLiability(String eventId, MarketType marketType, Selection selection) {
        return exposureRepository.findByEventIdAndMarketTypeAndSelection(eventId, marketType, selection)
                .map(Exposure::getReservedLiability)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getMaxReservedLiability(String eventId, MarketType marketType, Selection selection) {
        String selectionScope = eventId + "|" + marketType + "|" + selection;
        Optional<BigDecimal> fromSelection = limitRepository.findByScopeTypeAndScopeId(SCOPE_EVENT_MARKET_SELECTION, selectionScope)
                .stream()
                .map(Limit::getMaxReservedLiability)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder());

        Optional<BigDecimal> fromEvent = limitRepository.findByScopeTypeAndScopeId(SCOPE_EVENT, eventId)
                .stream()
                .map(Limit::getMaxReservedLiability)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder());

        Optional<BigDecimal> fromGlobal = limitRepository.findByScopeType(SCOPE_GLOBAL)
                .stream()
                .map(Limit::getMaxReservedLiability)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder());

        return fromSelection.or(() -> fromEvent).or(() -> fromGlobal).orElse(null);
    }

    private BigDecimal getMaxStakePerBet(String eventId, MarketType marketType, Selection selection) {
        String selectionScope = eventId + "|" + marketType + "|" + selection;
        Optional<BigDecimal> fromSelection = limitRepository.findByScopeTypeAndScopeId(SCOPE_EVENT_MARKET_SELECTION, selectionScope)
                .stream()
                .map(Limit::getMaxStakePerBet)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder());

        Optional<BigDecimal> fromEvent = limitRepository.findByScopeTypeAndScopeId(SCOPE_EVENT, eventId)
                .stream()
                .map(Limit::getMaxStakePerBet)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder());

        Optional<BigDecimal> fromGlobal = limitRepository.findByScopeType(SCOPE_GLOBAL)
                .stream()
                .map(Limit::getMaxStakePerBet)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder());

        return fromSelection.or(() -> fromEvent).or(() -> fromGlobal)
                .orElse(new BigDecimal("1000000"));  // Default high limit if none set
    }
}
