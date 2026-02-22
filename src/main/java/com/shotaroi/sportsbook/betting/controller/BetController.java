package com.shotaroi.sportsbook.betting.controller;

import com.shotaroi.sportsbook.betting.dto.PlaceBetRequest;
import com.shotaroi.sportsbook.betting.dto.PlaceBetResponse;
import com.shotaroi.sportsbook.betting.entity.Bet;
import com.shotaroi.sportsbook.betting.service.BetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bets")
@Tag(name = "Bets", description = "Public bet placement API (JWT)")
public class BetController {

    private final BetService betService;

    public BetController(BetService betService) {
        this.betService = betService;
    }

    @PostMapping
    @Operation(summary = "Place a bet", description = "Idempotency-Key header required")
    public ResponseEntity<PlaceBetResponse> placeBet(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody PlaceBetRequest request
    ) {
        PlaceBetResponse response = betService.placeBet(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bet by ID")
    public ResponseEntity<Bet> getBet(@PathVariable Long id) {
        Bet bet = betService.getBet(id);
        return ResponseEntity.ok(bet);
    }
}
