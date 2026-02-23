package com.shotaroi.sportsbook.common.error;

public class EventAlreadySettledException extends DomainException {

    private final String eventId;

    public EventAlreadySettledException(String eventId) {
        super("Event already settled: " + eventId);
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
