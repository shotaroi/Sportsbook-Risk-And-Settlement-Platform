package com.shotaroi.sportsbook.settlement.entity;

import com.shotaroi.sportsbook.common.domain.Selection;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "event_results")
public class EventResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "winning_selection")
    private Selection winningSelection;

    @Column(name = "result_version", nullable = false)
    private Long resultVersion = 1L;

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (settledAt == null) {
            settledAt = now;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Selection getWinningSelection() { return winningSelection; }
    public void setWinningSelection(Selection winningSelection) { this.winningSelection = winningSelection; }
    public Long getResultVersion() { return resultVersion; }
    public void setResultVersion(Long resultVersion) { this.resultVersion = resultVersion; }
    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
