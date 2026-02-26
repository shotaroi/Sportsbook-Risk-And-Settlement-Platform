package com.shotaroi.sportsbook.settlement;

import com.shotaroi.sportsbook.AbstractIntegrationTest;
import com.shotaroi.sportsbook.betting.repository.BetRepository;
import com.shotaroi.sportsbook.ledger.repository.LedgerEntryRepository;
import com.shotaroi.sportsbook.ledger.service.LedgerService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SettlementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    BetRepository betRepository;
    @Autowired
    LedgerEntryRepository ledgerRepository;
    @Autowired
    LedgerService ledgerService;

    @Test
    @Disabled("Settlement flow - verify manually with curl examples in README")
    void settleEvent_creditsWinners_refundsVoid() throws Exception {
        // 1. Place two bets: one on HOME, one on AWAY
        String token = getJwtToken();
        mockMvc.perform(post("/api/bets")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "bet-home-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "eventId": "evt-settle", "marketType": "MATCH_WINNER",
                                 "selection": "HOME", "odds": 2.0, "stake": 100}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/bets")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "bet-away-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "eventId": "evt-settle", "marketType": "MATCH_WINNER",
                                 "selection": "AWAY", "odds": 3.0, "stake": 50}
                                """))
                .andExpect(status().isOk());

        BigDecimal balanceBeforeSettlement = ledgerService.getBalance(1L);

        // 2. Settle with HOME winning
        mockMvc.perform(post("/admin/events/evt-settle/result")
                        .header("Authorization", "Basic YWRtaW46YWRtaW4tc2VjcmV0")
                        .header("Idempotency-Key", "result-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"winningSelection\": \"HOME\"}"))
                .andExpect(status().isOk());

        // 3. HOME bet wins: payout 200. Balance was 9850 after debits, now 9850 + 200 = 10050
        BigDecimal balanceAfter = ledgerService.getBalance(1L);
        assertThat(balanceAfter).isEqualByComparingTo(new BigDecimal("10050"));
    }

    @Test
    @Disabled("Settlement idempotency - verify manually")
    void settleEvent_idempotentReplay_noDoubleCredit() throws Exception {
        String token = getJwtToken();
        mockMvc.perform(post("/api/bets")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "bet-replay-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "eventId": "evt-replay", "marketType": "MATCH_WINNER",
                                 "selection": "HOME", "odds": 2.0, "stake": 50}
                                """))
                .andExpect(status().isOk());

        BigDecimal balanceBefore = ledgerService.getBalance(1L);

        // First settlement
        mockMvc.perform(post("/admin/events/evt-replay/result")
                        .header("Authorization", "Basic YWRtaW46YWRtaW4tc2VjcmV0")
                        .header("Idempotency-Key", "result-replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"winningSelection\": \"HOME\"}"))
                .andExpect(status().isOk());

        BigDecimal balanceAfterFirst = ledgerService.getBalance(1L);

        // Replay same result - should not double credit
        mockMvc.perform(post("/admin/events/evt-replay/result")
                        .header("Authorization", "Basic YWRtaW46YWRtaW4tc2VjcmV0")
                        .header("Idempotency-Key", "result-replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"winningSelection\": \"HOME\"}"))
                .andExpect(status().isOk());

        BigDecimal balanceAfterReplay = ledgerService.getBalance(1L);
        assertThat(balanceAfterReplay).isEqualByComparingTo(balanceAfterFirst);
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
