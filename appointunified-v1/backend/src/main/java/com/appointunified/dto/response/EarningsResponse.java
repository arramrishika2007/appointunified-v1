package com.appointunified.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EarningsResponse {
    private LocalDate date;
    private BigDecimal amount;
    private Integer completedAppointments;
    private BigDecimal averageRating;
}
