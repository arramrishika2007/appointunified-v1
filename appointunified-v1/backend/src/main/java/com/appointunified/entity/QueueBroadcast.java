package com.appointunified.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** NEW V3 FEATURE 5 */
@Entity @Table(name = "queue_broadcasts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QueueBroadcast {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "professional_id", nullable = false) private Professional professional;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
    @Column(name = "message_type", nullable = false, length = 20) private String messageType = "INFO";
    @Column(name = "sent_to_count", nullable = false) private Integer sentToCount = 0;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sent_by", nullable = false) private User sentBy;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
}
