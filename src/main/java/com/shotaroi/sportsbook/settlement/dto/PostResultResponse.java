package com.shotaroi.sportsbook.settlement.dto;

public record PostResultResponse(boolean success) {
    public static PostResultResponse ok() {
        return new PostResultResponse(true);
    }
}
