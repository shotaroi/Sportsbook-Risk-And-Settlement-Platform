package com.shotaroi.sportsbook.betting.entity;

import com.shotaroi.sportsbook.common.domain.BetStatus;
import com.shotaroi.sportsbook.common.domain.MarketType;
import com.shotaroi.sportsbook.common.domain.Selection;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bets")
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false)
    private MarketType marketType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Selection selection;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal odds;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal stake;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BetStatus status;

    @Column(name = "potential_payout", nullable = false, precision = 19, scale = 2)
    private BigDecimal potentialPayout;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "settlement_batch_id")
    private String settlementBatchId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public MarketType getMarketType() { return marketType; }
    public void setMarketType(MarketType marketType) { this.marketType = marketType; }
    public Selection getSelection() { return selection; }
    public void setSelection(Selection selection) { this.selection = selection; }
    public BigDecimal getOdds() { return odds; }
    public void setOdds(BigDecimal odds) { this.odds = odds; }
    public BigDecimal getStake() { return stake; }
    public void setStake(BigDecimal stake) { this.stake = stake; }
    public BetStatus getStatus() { return status; }
    public void setStatus(BetStatus status) { this.status = status; }
    public BigDecimal getPotentialPayout() { return potentialPayout; }
    public void setPotentialPayout(BigDecimal potentialPayout) { this.potentialPayout = potentialPayout; }
    public Instant getPlacedAt() { return placedAt; }
    public void setPlacedAt(Instant placedAt) { this.placedAt = placedAt; }
    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }
    public String getSettlementBatchId() { return settlementBatchId; }
    public void setSettlementBatchId(String settlementBatchId) { this.settlementBatchId = settlementBatchId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
