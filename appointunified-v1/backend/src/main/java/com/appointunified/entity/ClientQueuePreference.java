package com.appointunified.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** NEW V3 FEATURE 3 */
@Entity @Table(name = "client_queue_preferences")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ClientQueuePreference {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
    @Column(name = "notify_at_position", nullable = false) private Integer notifyAtPosition = 3;
    @Column(name = "prefer_sms_over_push", nullable = false) private boolean preferSmsOverPush = false;
    @Column(name = "auto_check_in_enabled", nullable = false) private boolean autoCheckInEnabled = true;
    @Column(name = "show_realtime_eta", nullable = false) private boolean showRealtimeEta = true;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt = OffsetDateTime.now();
}
