package com.appointunified.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "queue_tokens")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QueueToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @Column(name = "token_number", nullable = false)
    private Integer tokenNumber;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "estimated_wait_mins")
    private Integer estimatedWaitMins;

    /** WAITING | CALLED | IN_PROGRESS | COMPLETED | SKIPPED */
    @Column(nullable = false, length = 20)
    private String status = "WAITING";

    @Column(name = "called_at")    private OffsetDateTime calledAt;
    @Column(name = "completed_at") private OffsetDateTime completedAt;
    @Column(name = "skipped_at")   private OffsetDateTime skippedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
