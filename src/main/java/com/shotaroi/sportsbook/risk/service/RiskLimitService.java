package com.shotaroi.sportsbook.risk.service;

import com.shotaroi.sportsbook.risk.dto.RiskLimitRequest;
import com.shotaroi.sportsbook.risk.entity.Limit;
import com.shotaroi.sportsbook.risk.repository.LimitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RiskLimitService {

    private final LimitRepository limitRepository;

    public RiskLimitService(LimitRepository limitRepository) {
        this.limitRepository = limitRepository;
    }

    @Transactional
    public Limit setLimit(RiskLimitRequest request) {
        Optional<Limit> existing = limitRepository.findFirstByScopeTypeAndScopeId(
                request.scopeType(),
                request.scopeId()
        );

        Limit limit;
        if (existing.isPresent()) {
            limit = existing.get();
            limit.setMaxReservedLiability(request.maxReservedLiability());
            limit.setMaxStakePerBet(request.maxStakePerBet());
        } else {
            limit = new Limit();
            limit.setScopeType(request.scopeType());
            limit.setScopeId(request.scopeId());
            limit.setMaxReservedLiability(request.maxReservedLiability());
            limit.setMaxStakePerBet(request.maxStakePerBet());
        }
        return limitRepository.save(limit);
    }
}
