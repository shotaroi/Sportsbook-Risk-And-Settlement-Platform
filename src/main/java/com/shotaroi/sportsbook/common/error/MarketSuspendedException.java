package com.shotaroi.sportsbook.common.error;

public class MarketSuspendedException extends DomainException {

    private final String eventId;

    public MarketSuspendedException(String eventId) {
        super("Market suspended for event: " + eventId);
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
