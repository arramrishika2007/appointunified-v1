package com.appointunified.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sla_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SLAEvent {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(name = "breach_type", nullable = false)
    private String breachType;

    @Column(name = "breached_at", nullable = false)
    private LocalDateTime breachedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "escalated_to")
    private UUID escalatedTo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
