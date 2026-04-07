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
public class AnalyticsSummaryResponse {
    private Integer totalBookings;
    private Integer completedBookings;
    private Integer cancelledBookings;
    private Integer noShows;
    private BigDecimal totalRevenue;
    private Integer newUsers;
    private Integer newProfessionals;
    private BigDecimal averageNoShowRate;
    private BigDecimal averageCompletionRate;
    private String timeRange;  // '7d','30d','90d'
    private String sector;  // sector filter if applicable
}
