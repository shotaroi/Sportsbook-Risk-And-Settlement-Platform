package com.shotaroi.sportsbook.settlement.controller;

import com.shotaroi.sportsbook.settlement.dto.PostResultRequest;
import com.shotaroi.sportsbook.settlement.dto.PostResultResponse;
import com.shotaroi.sportsbook.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/events")
@Tag(name = "Admin - Settlement", description = "Admin result ingestion (Basic Auth)")
public class SettlementAdminController {

    private final SettlementService settlementService;

    public SettlementAdminController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/{eventId}/result")
    @Operation(summary = "Post event result", description = "Idempotency-Key header required. winningSelection=null for VOID.")
    public ResponseEntity<PostResultResponse> postResult(
            @PathVariable String eventId,
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody PostResultRequest request
    ) {
        PostResultResponse response = settlementService.postResult(eventId, request, idempotencyKey);
        return ResponseEntity.ok(response);
    }
}
