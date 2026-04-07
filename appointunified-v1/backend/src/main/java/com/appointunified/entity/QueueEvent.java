package com.appointunified.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity @Table(name = "queue_events")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QueueEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "professional_id", nullable = false) private Professional professional;
    @Column(name = "event_type", nullable = false, length = 30) private String eventType;
    @Column(name = "token_id") private UUID tokenId;
    @Column(name = "triggered_by") private UUID triggeredBy;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private Map<String, Object> payload;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
}
