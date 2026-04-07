package com.appointunified.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** NEW V3 FEATURE 1 */
@Entity @Table(name = "queue_analytics_snapshots")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QueueAnalyticsSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "professional_id", nullable = false) private Professional professional;
    @Column(name = "snapshot_at", nullable = false) private OffsetDateTime snapshotAt = OffsetDateTime.now();
    @Column(name = "queue_length", nullable = false) private Integer queueLength = 0;
    @Column(name = "current_wait_mins", nullable = false) private Integer currentWaitMins = 0;
    @Column(name = "total_served_today", nullable = false) private Integer totalServedToday = 0;
    @Column(name = "no_shows_today", nullable = false) private Integer noShowsToday = 0;
    @Column(name = "avg_service_mins", precision = 6, scale = 2) private BigDecimal avgServiceMins;
    @Column(name = "is_paused", nullable = false) private boolean paused = false;
    @Column(name = "cumulative_delay_mins", nullable = false) private Integer cumulativeDelayMins = 0;
}
