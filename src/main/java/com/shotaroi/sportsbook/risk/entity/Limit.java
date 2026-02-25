package com.shotaroi.sportsbook.risk.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "limits")
public class Limit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope_type", nullable = false)
    private String scopeType;

    @Column(name = "scope_id")
    private String scopeId;

    @Column(name = "max_reserved_liability", precision = 19, scale = 2)
    private BigDecimal maxReservedLiability;

    @Column(name = "max_stake_per_bet", precision = 19, scale = 2)
    private BigDecimal maxStakePerBet;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getScopeId() { return scopeId; }
    public void setScopeId(String scopeId) { this.scopeId = scopeId; }
    public BigDecimal getMaxReservedLiability() { return maxReservedLiability; }
    public void setMaxReservedLiability(BigDecimal maxReservedLiability) { this.maxReservedLiability = maxReservedLiability; }
    public BigDecimal getMaxStakePerBet() { return maxStakePerBet; }
    public void setMaxStakePerBet(BigDecimal maxStakePerBet) { this.maxStakePerBet = maxStakePerBet; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
