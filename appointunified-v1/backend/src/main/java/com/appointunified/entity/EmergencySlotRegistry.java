package com.appointunified.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** NEW V3 FEATURE 4 */
@Entity @Table(name = "emergency_slot_registry")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmergencySlotRegistry {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "appointment_id", nullable = false) private Appointment appointment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "professional_id", nullable = false) private Professional professional;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "inserted_by", nullable = false) private User insertedBy;
    @Column(nullable = false, columnDefinition = "TEXT") private String justification;
    @Column(name = "bumped_tokens", columnDefinition = "TEXT") private String bumpedTokens = "{}";
    @Column(name = "inserted_at", nullable = false, updatable = false) private OffsetDateTime insertedAt = OffsetDateTime.now();
}
