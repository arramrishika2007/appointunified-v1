package com.appointunified.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NoShowTrendResponse {
    private UUID professionalId;
    private String professionalName;
    private LocalDate date;
    private Integer totalAppointments;
    private Integer noShowCount;
    private BigDecimal noShowRate;  // percentage 0-100
    private String trend;  // 'UP','DOWN','STABLE'
}
