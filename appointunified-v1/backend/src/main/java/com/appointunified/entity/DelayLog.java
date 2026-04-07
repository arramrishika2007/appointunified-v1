package com.appointunified.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "delay_log")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DelayLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "professional_id", nullable = false) private Professional professional;
    @Column(name = "delay_minutes", nullable = false) private Integer delayMinutes;
    @Column(length = 255) private String reason;
    @Column(name = "tokens_affected", nullable = false) private Integer tokensAffected = 0;
    @Column(name = "triggered_by") private UUID triggeredBy;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
}
