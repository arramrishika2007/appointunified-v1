package com.appointunified.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** NEW V3 FEATURE 2 */
@Entity @Table(name = "queue_pause_log")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QueuePauseLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "professional_id", nullable = false) private Professional professional;
    @Column(length = 255) private String reason;
    @Column(name = "paused_at", nullable = false) private OffsetDateTime pausedAt = OffsetDateTime.now();
    @Column(name = "resumed_at") private OffsetDateTime resumedAt;
    @Column(name = "pause_duration_secs") private Integer pauseDurationSecs;
    @Column(name = "triggered_by") private UUID triggeredBy;
}
