package com.appointunified.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reputation_scores")
@Getter
@Setter
public class ReputationScore {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false, unique = true)
    private Professional professional;

    @Column(name = "overall_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal overallScore = BigDecimal.ZERO;

    @Column(name = "rating_component", nullable = false, precision = 4, scale = 2)
    private BigDecimal ratingComponent = BigDecimal.ZERO;

    @Column(name = "completion_component", nullable = false, precision = 4, scale = 2)
    private BigDecimal completionComponent = BigDecimal.ZERO;

    @Column(name = "response_component", nullable = false, precision = 4, scale = 2)
    private BigDecimal responseComponent = BigDecimal.ZERO;

    @Column(name = "recency_component", nullable = false, precision = 4, scale = 2)
    private BigDecimal recencyComponent = BigDecimal.ZERO;

    @Column(name = "total_appointments", nullable = false)
    private Integer totalAppointments = 0;

    @Column(name = "no_show_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal noShowRate = BigDecimal.ZERO;

    @Column(name = "cancellation_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal cancellationRate = BigDecimal.ZERO;

    @Column(name = "avg_response_hours", precision = 6, scale = 2)
    private BigDecimal avgResponseHours;

    @Column(name = "last_computed_at", nullable = false)
    private OffsetDateTime lastComputedAt = OffsetDateTime.now();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
