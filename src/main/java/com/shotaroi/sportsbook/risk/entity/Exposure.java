package com.shotaroi.sportsbook.risk.entity;

import com.shotaroi.sportsbook.common.domain.MarketType;
import com.shotaroi.sportsbook.common.domain.Selection;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "exposures", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"event_id", "market_type", "selection"})
})
public class Exposure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false)
    private MarketType marketType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Selection selection;

    @Column(name = "reserved_liability", nullable = false, precision = 19, scale = 2)
    private BigDecimal reservedLiability = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        updatedAt = Instant.now();
        if (version == null) {
            version = 0L;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public MarketType getMarketType() { return marketType; }
    public void setMarketType(MarketType marketType) { this.marketType = marketType; }
    public Selection getSelection() { return selection; }
    public void setSelection(Selection selection) { this.selection = selection; }
    public BigDecimal getReservedLiability() { return reservedLiability; }
    public void setReservedLiability(BigDecimal reservedLiability) { this.reservedLiability = reservedLiability; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
