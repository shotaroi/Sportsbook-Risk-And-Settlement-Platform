package com.shotaroi.sportsbook.betting.repository;

import com.shotaroi.sportsbook.betting.entity.Bet;
import com.shotaroi.sportsbook.common.domain.BetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BetRepository extends JpaRepository<Bet, Long> {

    List<Bet> findByCustomerIdOrderByPlacedAtDesc(Long customerId, org.springframework.data.domain.Pageable pageable);

    List<Bet> findByEventIdAndStatus(String eventId, BetStatus status);
}
