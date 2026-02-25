package com.shotaroi.sportsbook.risk.repository;

import com.shotaroi.sportsbook.risk.entity.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LimitRepository extends JpaRepository<Limit, Long> {

    List<Limit> findByScopeType(String scopeType);

    List<Limit> findByScopeTypeAndScopeId(String scopeType, String scopeId);

    Optional<Limit> findFirstByScopeTypeAndScopeId(String scopeType, String scopeId);
}
