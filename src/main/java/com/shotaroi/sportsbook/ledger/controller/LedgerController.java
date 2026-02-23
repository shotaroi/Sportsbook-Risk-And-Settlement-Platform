package com.shotaroi.sportsbook.ledger.controller;

import com.shotaroi.sportsbook.ledger.dto.LedgerEntryDto;
import com.shotaroi.sportsbook.ledger.entity.LedgerEntry;
import com.shotaroi.sportsbook.ledger.repository.LedgerEntryRepository;
import com.shotaroi.sportsbook.ledger.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/customers/{customerId}/ledger")
@Tag(name = "Ledger", description = "Customer ledger (JWT)")
public class LedgerController {

    private final LedgerService ledgerService;
    private final LedgerEntryRepository ledgerRepository;

    public LedgerController(LedgerService ledgerService, LedgerEntryRepository ledgerRepository) {
        this.ledgerService = ledgerService;
        this.ledgerRepository = ledgerRepository;
    }

    @GetMapping
    @Operation(summary = "Get ledger entries with pagination")
    public ResponseEntity<Page<LedgerEntryDto>> getLedger(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<LedgerEntry> entries = ledgerRepository.findByCustomerIdOrderByCreatedAtDesc(
                customerId, PageRequest.of(page, size));
        return ResponseEntity.ok(entries.map(this::toDto));
    }

    @GetMapping("/balance")
    @Operation(summary = "Get current balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(@PathVariable Long customerId) {
        BigDecimal balance = ledgerService.getBalance(customerId);
        return ResponseEntity.ok(Map.of("balance", balance));
    }

    private LedgerEntryDto toDto(LedgerEntry e) {
        return new LedgerEntryDto(
                e.getId(),
                e.getType(),
                e.getAmount(),
                e.getCurrency(),
                e.getReferenceType(),
                e.getReferenceId(),
                e.getCreatedAt()
        );
    }
}
