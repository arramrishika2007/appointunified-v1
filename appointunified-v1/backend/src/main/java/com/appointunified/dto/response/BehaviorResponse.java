package com.appointunified.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class BehaviorResponse {

    public static class RiskScore {
        private UUID userId;
        private BigDecimal score;
        private String riskLevel;

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    }

    public static class RiskSummary {
        private UUID userId;
        private BigDecimal score;
        private String riskLevel;
        private int totalCancellations;
        private int lastMinuteCancellations;
        private int noShows;
        private int completions;
        private OffsetDateTime lastCalculatedAt;

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public int getTotalCancellations() { return totalCancellations; }
        public void setTotalCancellations(int totalCancellations) { this.totalCancellations = totalCancellations; }
        public int getLastMinuteCancellations() { return lastMinuteCancellations; }
        public void setLastMinuteCancellations(int lastMinuteCancellations) { this.lastMinuteCancellations = lastMinuteCancellations; }
        public int getNoShows() { return noShows; }
        public void setNoShows(int noShows) { this.noShows = noShows; }
        public int getCompletions() { return completions; }
        public void setCompletions(int completions) { this.completions = completions; }
        public OffsetDateTime getLastCalculatedAt() { return lastCalculatedAt; }
        public void setLastCalculatedAt(OffsetDateTime lastCalculatedAt) { this.lastCalculatedAt = lastCalculatedAt; }
    }
}
