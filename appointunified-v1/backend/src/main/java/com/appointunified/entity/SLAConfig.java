package com.appointunified.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sla_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SLAConfig {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "sector", nullable = false)
    private String sector;

    @Column(name = "priority_tier", nullable = false)
    private String priorityTier;

    @Column(name = "max_wait_minutes", nullable = false)
    private Integer maxWaitMinutes;

    @Column(name = "escalation_target", nullable = false)
    private String escalationTarget;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
