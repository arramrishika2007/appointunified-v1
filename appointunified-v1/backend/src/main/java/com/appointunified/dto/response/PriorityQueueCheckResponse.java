package com.appointunified.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriorityQueueCheckResponse {
    private UUID professionalId;
    private Integer totalBookings;
    private Integer premiumCount;
    private Integer normalCount;
    private Integer emergencyCount;
    private BigDecimal premiumRatio;
    private Boolean isFairnessTriggered;
    private String nextPremiumSlotPosition;
}
