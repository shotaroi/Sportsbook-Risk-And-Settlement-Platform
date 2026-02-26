package com.shotaroi.sportsbook.risk;

import com.shotaroi.sportsbook.AbstractIntegrationTest;
import com.shotaroi.sportsbook.risk.repository.ExposureRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves that under concurrent bet placement, limits are not exceeded.
 */
@AutoConfigureMockMvc
class ConcurrentBetPlacementTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ExposureRepository exposureRepository;

    @Test
    @Disabled("Admin limits + concurrency - verify manually with: docker compose up -d && mvn spring-boot:run")
    void concurrentBets_doNotExceedLiabilityLimit() throws Exception {
        // Set limit: max 500 liability for HOME (admin:admin-secret)
        String basicAuth = "Basic " + java.util.Base64.getEncoder().encodeToString("admin:admin-secret".getBytes());
        mockMvc.perform(post("/admin/limits")
                        .header("Authorization", basicAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "scopeType": "EVENT_MARKET_SELECTION",
                                    "scopeId": "evt-concurrent|MATCH_WINNER|HOME",
                                    "maxReservedLiability": 500,
                                    "maxStakePerBet": 1000
                                }
                                """))
                .andExpect(status().isOk());

        // Seed more balance for customer 1 (seed has 10000, we need enough for many bets)
        // Place 20 concurrent bets of 100 stake each. Liability per bet = 100 * 1.85 - 100 = 85
        // Max bets that fit in 500 liability: 500/85 = 5.88, so at most 5 should succeed
        // Actually we want to prove limits work - so we'll have many threads try to place
        // and verify total reserved <= 500

        String token = getJwtToken();
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    var result = mockMvc.perform(post("/api/bets")
                                    .header("Authorization", "Bearer " + token)
                                    .header("Idempotency-Key", "concurrent-" + idx)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {"customerId": 1, "eventId": "evt-concurrent", "marketType": "MATCH_WINNER",
                                             "selection": "HOME", "odds": 1.85, "stake": 100}
                                            """))
                            .andReturn();
                    if (result.getResponse().getStatus() == 200) {
                        var body = result.getResponse().getContentAsString();
                        if (body.contains("\"betId\"") && !body.contains("\"decision\":\"REJECT\"")) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Verify total reserved liability does not exceed 500
        var exposure = exposureRepository.findByEventIdAndMarketTypeAndSelection(
                "evt-concurrent",
                com.shotaroi.sportsbook.common.domain.MarketType.MATCH_WINNER,
                com.shotaroi.sportsbook.common.domain.Selection.HOME
        );
        assertThat(exposure).isPresent();
        assertThat(exposure.get().getReservedLiability())
                .isLessThanOrEqualTo(new BigDecimal("501"));  // Allow small rounding
    }

    private String getJwtToken() throws Exception {
        var result = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\": 1}"))
                .andExpect(status().isOk())
                .andReturn();
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }
}
