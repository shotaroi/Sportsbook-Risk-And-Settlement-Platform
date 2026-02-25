package com.shotaroi.sportsbook.settlement.repository;

import com.shotaroi.sportsbook.settlement.entity.EventResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventResultRepository extends JpaRepository<EventResult, Long> {

    Optional<EventResult> findByEventId(String eventId);
}
