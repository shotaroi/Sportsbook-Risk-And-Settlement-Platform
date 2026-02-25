package com.shotaroi.sportsbook.risk.service;

import com.shotaroi.sportsbook.common.domain.MarketType;
import com.shotaroi.sportsbook.common.domain.Selection;
import com.shotaroi.sportsbook.common.util.MoneyUtil;
import com.shotaroi.sportsbook.risk.entity.Exposure;
import com.shotaroi.sportsbook.risk.repository.ExposureRepository;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Reserves and releases exposure with optimistic locking + retry.
 * Prevents overshooting limits under concurrent bet placements.
 */
@Service
public class RiskReservationService {

    private static final Logger log = LoggerFactory.getLogger(RiskReservationService.class);
    private static final int MAX_RETRIES = 10;

    private final ExposureRepository exposureRepository;

    public RiskReservationService(ExposureRepository exposureRepository) {
        this.exposureRepository = exposureRepository;
    }

    /**
     * Reserve liability atomically. Retries on OptimisticLockException.
     */
    @Transactional
    public void reserveLiability(String eventId, MarketType marketType, Selection selection, BigDecimal liability) {
        MoneyUtil.validatePositiveStake(liability);
        int attempts = 0;
        while (true) {
            try {
                doReserve(eventId, marketType, selection, liability);
                return;
            } catch (OptimisticLockException e) {
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    log.error("Max retries exceeded for exposure reservation: eventId={}, selection={}", eventId, selection);
                    throw e;
                }
                log.debug("Optimistic lock conflict, retry {}/{}: eventId={}", attempts, MAX_RETRIES, eventId);
            }
        }
    }

    private void doReserve(String eventId, MarketType marketType, Selection selection, BigDecimal liability) {
        Exposure exposure = exposureRepository.findByEventIdAndMarketTypeAndSelectionForUpdate(eventId, marketType, selection)
                .orElseGet(() -> {
                    Exposure newExp = new Exposure();
                    newExp.setEventId(eventId);
                    newExp.setMarketType(marketType);
                    newExp.setSelection(selection);
                    newExp.setReservedLiability(BigDecimal.ZERO);
                    newExp.setVersion(0L);
                    return exposureRepository.save(newExp);
                });

        exposure.setReservedLiability(exposure.getReservedLiability().add(MoneyUtil.money(liability)));
        exposureRepository.save(exposure);
    }

    /**
     * Release liability (on settlement). Retries on OptimisticLockException.
     */
    @Transactional
    public void releaseLiability(String eventId, MarketType marketType, Selection selection, BigDecimal liability) {
        if (liability == null || liability.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        int attempts = 0;
        while (true) {
            try {
                doRelease(eventId, marketType, selection, liability);
                return;
            } catch (OptimisticLockException e) {
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    log.error("Max retries exceeded for exposure release: eventId={}, selection={}", eventId, selection);
                    throw e;
                }
                log.debug("Optimistic lock conflict on release, retry {}/{}", attempts, MAX_RETRIES);
            }
        }
    }

    private void doRelease(String eventId, MarketType marketType, Selection selection, BigDecimal liability) {
        Exposure exposure = exposureRepository.findByEventIdAndMarketTypeAndSelectionForUpdate(eventId, marketType, selection)
                .orElseThrow(() -> new IllegalStateException("Exposure not found for release: " + eventId + "/" + selection));

        BigDecimal newLiability = exposure.getReservedLiability().subtract(MoneyUtil.money(liability));
        if (newLiability.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Exposure would go negative, clamping to zero: eventId={}, selection={}", eventId, selection);
            newLiability = BigDecimal.ZERO;
        }
        exposure.setReservedLiability(newLiability);
        exposureRepository.save(exposure);
    }
}
