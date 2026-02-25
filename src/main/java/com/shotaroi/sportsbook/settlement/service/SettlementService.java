package com.shotaroi.sportsbook.settlement.service;

import com.shotaroi.sportsbook.betting.entity.Bet;
import com.shotaroi.sportsbook.betting.repository.BetRepository;
import com.shotaroi.sportsbook.common.domain.BetStatus;
import com.shotaroi.sportsbook.common.domain.IdempotencyScope;
import com.shotaroi.sportsbook.common.domain.LedgerReferenceType;
import com.shotaroi.sportsbook.common.domain.Selection;
import com.shotaroi.sportsbook.common.idempotency.IdempotencyService;
import com.shotaroi.sportsbook.common.util.MoneyUtil;
import com.shotaroi.sportsbook.ledger.service.LedgerService;
import com.shotaroi.sportsbook.risk.service.RiskReservationService;
import com.shotaroi.sportsbook.settlement.dto.PostResultRequest;
import com.shotaroi.sportsbook.settlement.dto.PostResultResponse;
import com.shotaroi.sportsbook.settlement.entity.EventResult;
import com.shotaroi.sportsbook.settlement.repository.EventResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Settlement engine: idempotently record result, settle bets, credit/refund, release exposure.
 */
@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final EventResultRepository eventResultRepository;
    private final BetRepository betRepository;
    private final LedgerService ledgerService;
    private final RiskReservationService riskReservationService;
    private final IdempotencyService idempotencyService;

    public SettlementService(EventResultRepository eventResultRepository,
                             BetRepository betRepository,
                             LedgerService ledgerService,
                             RiskReservationService riskReservationService,
                             IdempotencyService idempotencyService) {
        this.eventResultRepository = eventResultRepository;
        this.betRepository = betRepository;
        this.ledgerService = ledgerService;
        this.riskReservationService = riskReservationService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public PostResultResponse postResult(String eventId, PostResultRequest request, String idempotencyKey) {
        return idempotencyService.execute(
                IdempotencyScope.RESULT_INGEST,
                eventId,
                idempotencyKey,
                new ResultIngestRequest(eventId, request),
                PostResultResponse.class,
                () -> {
                    doPostResult(eventId, request);
                    return PostResultResponse.ok();
                }
        );
    }

    private void doPostResult(String eventId, PostResultRequest request) {
        // 1. Idempotently record result (unique by eventId)
        EventResult result = eventResultRepository.findByEventId(eventId).orElse(null);
        if (result != null) {
            log.info("Result already recorded for eventId={}, skipping (idempotent)", eventId);
            return;
        }

        result = new EventResult();
        result.setEventId(eventId);
        result.setWinningSelection(request.winningSelection());
        result.setResultVersion(1L);
        result.setSettledAt(Instant.now());
        eventResultRepository.save(result);

        // 2. Find all PLACED bets for event
        List<Bet> bets = betRepository.findByEventIdAndStatus(eventId, BetStatus.PLACED);
        String batchId = "BATCH-" + UUID.randomUUID();

        log.info("Settling eventId={}, batchId={}, bets={}, result={}",
                eventId, batchId, bets.size(), request.winningSelection());

        for (Bet bet : bets) {
            settleBet(bet, request.winningSelection(), batchId);
        }
    }

    private void settleBet(Bet bet, Selection winningSelection, String batchId) {
        BetStatus newStatus;
        Instant settledAt = Instant.now();

        if (winningSelection == null) {
            // VOID: refund stake
            newStatus = BetStatus.SETTLED_VOID;
            ledgerService.refundStake(bet.getCustomerId(), bet.getStake(), "BET-" + bet.getId());
        } else if (bet.getSelection() == winningSelection) {
            // WON: credit payout
            newStatus = BetStatus.SETTLED_WON;
            ledgerService.creditPayout(bet.getCustomerId(), bet.getPotentialPayout(), "BET-" + bet.getId());
        } else {
            // LOST: no credit
            newStatus = BetStatus.SETTLED_LOST;
        }

        // Release exposure (liability = potentialPayout - stake)
        BigDecimal liability = bet.getPotentialPayout().subtract(bet.getStake());
        riskReservationService.releaseLiability(
                bet.getEventId(),
                bet.getMarketType(),
                bet.getSelection(),
                liability
        );

        bet.setStatus(newStatus);
        bet.setSettledAt(settledAt);
        bet.setSettlementBatchId(batchId);
        betRepository.save(bet);
    }

    private record ResultIngestRequest(String eventId, PostResultRequest request) {}
}
