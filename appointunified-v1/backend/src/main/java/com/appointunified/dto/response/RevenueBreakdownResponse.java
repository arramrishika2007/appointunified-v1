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
public class RevenueBreakdownResponse {
    private String serviceType;  // service name or category
    private BigDecimal amount;
    private Integer bookingCount;
    private String sector;  // applicable sector
    private BigDecimal percentageOfTotal;
}
