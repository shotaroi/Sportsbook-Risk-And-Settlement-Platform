package com.shotaroi.sportsbook.risk.controller;

import com.shotaroi.sportsbook.risk.dto.ExposureDto;
import com.shotaroi.sportsbook.risk.dto.RiskLimitRequest;
import com.shotaroi.sportsbook.risk.entity.Exposure;
import com.shotaroi.sportsbook.risk.entity.Limit;
import com.shotaroi.sportsbook.risk.repository.ExposureRepository;
import com.shotaroi.sportsbook.risk.repository.LimitRepository;
import com.shotaroi.sportsbook.risk.service.RiskLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin - Risk", description = "Admin risk limits and exposures (Basic Auth)")
public class RiskAdminController {

    private final ExposureRepository exposureRepository;
    private final LimitRepository limitRepository;
    private final RiskLimitService riskLimitService;

    public RiskAdminController(ExposureRepository exposureRepository,
                                LimitRepository limitRepository,
                                RiskLimitService riskLimitService) {
        this.exposureRepository = exposureRepository;
        this.limitRepository = limitRepository;
        this.riskLimitService = riskLimitService;
    }

    @GetMapping("/exposures")
    @Operation(summary = "Get exposures, optionally filtered by eventId")
    public ResponseEntity<List<ExposureDto>> getExposures(
            @RequestParam(required = false) String eventId
    ) {
        List<Exposure> exposures = eventId != null
                ? exposureRepository.findByEventId(eventId)
                : exposureRepository.findAll();
        return ResponseEntity.ok(exposures.stream().map(this::toDto).toList());
    }

    @PostMapping("/limits")
    @Operation(summary = "Set or update risk limit")
    public ResponseEntity<Limit> setLimit(@Valid @RequestBody RiskLimitRequest request) {
        Limit limit = riskLimitService.setLimit(request);
        return ResponseEntity.ok(limit);
    }

    private ExposureDto toDto(Exposure e) {
        return new ExposureDto(
                e.getId(),
                e.getEventId(),
                e.getMarketType(),
                e.getSelection(),
                e.getReservedLiability()
        );
    }
}
