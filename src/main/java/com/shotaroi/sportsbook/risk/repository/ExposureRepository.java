package com.shotaroi.sportsbook.risk.repository;

import com.shotaroi.sportsbook.common.domain.MarketType;
import com.shotaroi.sportsbook.common.domain.Selection;
import com.shotaroi.sportsbook.risk.entity.Exposure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ExposureRepository extends JpaRepository<Exposure, Long> {

    Optional<Exposure> findByEventIdAndMarketTypeAndSelection(String eventId, MarketType marketType, Selection selection);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT e FROM Exposure e WHERE e.eventId = :eventId AND e.marketType = :marketType AND e.selection = :selection")
    Optional<Exposure> findByEventIdAndMarketTypeAndSelectionForUpdate(
            @Param("eventId") String eventId,
            @Param("marketType") MarketType marketType,
            @Param("selection") Selection selection
    );

    List<Exposure> findByEventId(String eventId);
}
