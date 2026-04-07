package com.appointunified.entity;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "waitlist")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private com.appointunified.entity.Service service;

    @Column(name = "preferred_time_from")
    private LocalTime preferredTimeFrom;

    @Column(name = "preferred_time_to")
    private LocalTime preferredTimeTo;

    @Column(nullable = false)
    private boolean notified = false;

    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Professional getProfessional() { return professional; }
    public void setProfessional(Professional professional) { this.professional = professional; }
    public com.appointunified.entity.Service getService() { return service; }
    public void setService(com.appointunified.entity.Service service) { this.service = service; }
    public LocalTime getPreferredTimeFrom() { return preferredTimeFrom; }
    public void setPreferredTimeFrom(LocalTime preferredTimeFrom) { this.preferredTimeFrom = preferredTimeFrom; }
    public LocalTime getPreferredTimeTo() { return preferredTimeTo; }
    public void setPreferredTimeTo(LocalTime preferredTimeTo) { this.preferredTimeTo = preferredTimeTo; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
    public OffsetDateTime getNotifiedAt() { return notifiedAt; }
    public void setNotifiedAt(OffsetDateTime notifiedAt) { this.notifiedAt = notifiedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}