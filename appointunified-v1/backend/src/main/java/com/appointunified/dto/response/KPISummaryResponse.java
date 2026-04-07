package com.appointunified.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KPISummaryResponse {
    private Integer totalActiveUsers;
    private Integer totalActiveProfessionals;
    private Integer totalBookingsToday;
    private Integer completedBookingsToday;
    private BigDecimal platformRevenueToday;
    private BigDecimal averageNoShowRateToday;
    private Integer openSLABreachesCount;
    private Long redisQueueDepth;
    private Integer activeWebSocketConnections;
    private String generatedAt;
}
