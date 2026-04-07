package com.appointunified.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

// NEW V1 FEATURE 3: Booking Intent Drafts
@Entity
@Table(name = "booking_drafts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingDraft {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id")
    private Professional professional;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "draft_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> draftData;
    @Column(name = "step_reached", nullable = false)
    private Short stepReached = 1;
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Professional getProfessional() { return professional; }
    public void setProfessional(Professional professional) { this.professional = professional; }
    public Service getService() { return service; }
    public void setService(Service service) { this.service = service; }
    public Map<String, Object> getDraftData() { return draftData; }
    public void setDraftData(Map<String, Object> draftData) { this.draftData = draftData; }
    public Short getStepReached() { return stepReached; }
    public void setStepReached(Short stepReached) { this.stepReached = stepReached; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
