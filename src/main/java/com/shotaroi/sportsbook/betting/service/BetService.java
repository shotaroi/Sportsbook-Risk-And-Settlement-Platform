package com.shotaroi.sportsbook.betting.service;

import com.shotaroi.sportsbook.betting.dto.PlaceBetRequest;
import com.shotaroi.sportsbook.betting.dto.PlaceBetResponse;
import com.shotaroi.sportsbook.betting.entity.Bet;
import com.shotaroi.sportsbook.betting.repository.BetRepository;
import com.shotaroi.sportsbook.betting.repository.CustomerRepository;
import com.shotaroi.sportsbook.common.domain.BetStatus;
import com.shotaroi.sportsbook.common.domain.IdempotencyScope;
import com.shotaroi.sportsbook.common.domain.MarketType;
import com.shotaroi.sportsbook.common.domain.RiskDecision;
import com.shotaroi.sportsbook.common.error.ResourceNotFoundException;
import com.shotaroi.sportsbook.common.idempotency.IdempotencyService;
import com.shotaroi.sportsbook.common.util.MoneyUtil;
import com.shotaroi.sportsbook.ledger.service.LedgerService;
import com.shotaroi.sportsbook.risk.dto.RiskDecisionResult;
import com.shotaroi.sportsbook.risk.service.RiskEngineService;
import com.shotaroi.sportsbook.risk.service.RiskReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Place bet flow: validate -> risk check -> reserve exposure -> debit -> persist bet.
 */
@Service
public class BetService {

    private static final Logger log = LoggerFactory.getLogger(BetService.class);

    private final BetRepository betRepository;
    private final CustomerRepository customerRepository;
    private final RiskEngineService riskEngineService;
    private final RiskReservationService riskReservationService;
    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;

    public BetService(BetRepository betRepository,
                      CustomerRepository customerRepository,
                      RiskEngineService riskEngineService,
                      RiskReservationService riskReservationService,
                      LedgerService ledgerService,
                      IdempotencyService idempotencyService) {
        this.betRepository = betRepository;
        this.customerRepository = customerRepository;
        this.riskEngineService = riskEngineService;
        this.riskReservationService = riskReservationService;
        this.ledgerService = ledgerService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public PlaceBetResponse placeBet(PlaceBetRequest request, String idempotencyKey) {
        return idempotencyService.execute(
                IdempotencyScope.BET_PLACEMENT,
                String.valueOf(request.customerId()),
                idempotencyKey,
                request,
                PlaceBetResponse.class,
                () -> doPlaceBet(request)
        );
    }

    private PlaceBetResponse doPlaceBet(PlaceBetRequest request) {
        // 1. Validate customer exists
        if (!customerRepository.existsById(request.customerId())) {
            throw new ResourceNotFoundException("Customer", request.customerId());
        }

        // 2. Validate market (MVP: accept; no market suspension check)
        MoneyUtil.validateOdds(request.odds());
        MoneyUtil.validatePositiveStake(request.stake());

        // 3. Risk engine decision
        BigDecimal potentialLiability = MoneyUtil.potentialPayout(request.stake(), request.odds())
                .subtract(request.stake());
        RiskDecisionResult riskResult = riskEngineService.evaluate(
                request.eventId(),
                request.marketType(),
                request.selection(),
                request.stake(),
                potentialLiability
        );

        BigDecimal acceptedStake = riskResult.maxAllowedStake();
        if (riskResult.decision() == RiskDecision.REJECT) {
            log.info("Bet rejected: customerId={}, eventId={}, reason={}",
                    request.customerId(), request.eventId(), riskResult.rejectReason());
            return new PlaceBetResponse(null, null, BigDecimal.ZERO, BigDecimal.ZERO, RiskDecision.REJECT);
        }

        if (acceptedStake.compareTo(request.stake()) < 0) {
            acceptedStake = riskResult.maxAllowedStake();
        }

        BigDecimal potentialPayout = MoneyUtil.potentialPayout(acceptedStake, request.odds());
        BigDecimal liabilityToReserve = potentialPayout.subtract(acceptedStake);

        // 4. Reserve exposure (optimistic locking + retry)
        riskReservationService.reserveLiability(
                request.eventId(),
                request.marketType(),
                request.selection(),
                liabilityToReserve
        );

        try {
            // 5. Debit stake from wallet (may throw InsufficientFundsException)
            Bet bet = new Bet();
        bet.setCustomerId(request.customerId());
        bet.setEventId(request.eventId());
        bet.setMarketType(request.marketType());
        bet.setSelection(request.selection());
        bet.setOdds(MoneyUtil.odds(request.odds()));
        bet.setStake(MoneyUtil.money(acceptedStake));
        bet.setStatus(BetStatus.PLACED);
        bet.setPotentialPayout(potentialPayout);
        bet.setPlacedAt(Instant.now());
            bet = betRepository.save(bet);
            ledgerService.debitStake(request.customerId(), acceptedStake, "BET-" + bet.getId());

            log.info("Bet placed: betId={}, customerId={}, eventId={}, stake={}, decision={}",
                    bet.getId(), request.customerId(), request.eventId(), acceptedStake, riskResult.decision());

            return new PlaceBetResponse(
                bet.getId(),
                BetStatus.PLACED,
                acceptedStake,
                potentialPayout,
                riskResult.decision()
            );
        } catch (Exception e) {
            // Compensate: release reserved exposure if debit fails
            riskReservationService.releaseLiability(
                    request.eventId(),
                    request.marketType(),
                    request.selection(),
                    liabilityToReserve
            );
            throw e;
        }
    }

    public Bet getBet(Long id) {
        return betRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bet", id));
    }
}
