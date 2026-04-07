package com.appointunified.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "priority_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriorityRule {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(name = "max_premium_ratio", precision = 3, scale = 2)
    private BigDecimal maxPremiumRatio = new BigDecimal("0.40");

    @Column(name = "emergency_fee_multiplier", precision = 3, scale = 2)
    private BigDecimal emergencyFeeMultiplier = BigDecimal.ZERO;

    @Column(name = "premium_fee_multiplier", precision = 3, scale = 2)
    private BigDecimal premiumFeeMultiplier = new BigDecimal("0.30");

    @Column(name = "sla_warn_minutes")
    private Integer slaWarnMinutes = 15;

    @Column(name = "sla_escalate_minutes")
    private Integer slaEscalateMinutes = 30;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
