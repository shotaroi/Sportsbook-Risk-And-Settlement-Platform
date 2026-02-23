package com.shotaroi.sportsbook.common.repository;

import com.shotaroi.sportsbook.common.domain.IdempotencyScope;
import com.shotaroi.sportsbook.common.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByScopeAndScopeIdAndIdempotencyKey(
            IdempotencyScope scope,
            String scopeId,
            String idempotencyKey
    );
}
