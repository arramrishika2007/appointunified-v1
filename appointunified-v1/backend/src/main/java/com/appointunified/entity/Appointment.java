package com.appointunified.entity;

import com.appointunified.enums.AppointmentPriority;
import com.appointunified.enums.AppointmentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "appointment_status")
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "priority", nullable = false, columnDefinition = "appointment_priority")
    private AppointmentPriority priority = AppointmentPriority.NORMAL;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "client_notes", columnDefinition = "TEXT")
    private String clientNotes;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @Column(name = "is_virtual", nullable = false)
    private boolean virtual = false;

    @Column(name = "meet_link")
    private String meetLink;

    @Column(name = "meeting_token", length = 10)
    private String meetingToken;

    @Column(name = "workflow_instance_id")
    private UUID workflowInstanceId;

    @Column(name = "client_lat")
    private Double clientLat;

    @Column(name = "client_lon")
    private Double clientLon;

    @Column(name = "distance_meters")
    private Integer distanceMeters;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "deposit_status", columnDefinition = "payment_status")
    private com.appointunified.enums.PaymentStatus depositStatus = com.appointunified.enums.PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "final_payment_status", columnDefinition = "payment_status")
    private com.appointunified.enums.PaymentStatus finalPaymentStatus = com.appointunified.enums.PaymentStatus.PENDING;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private java.math.BigDecimal totalAmount;

    @Column(name = "premium_fee", precision = 10, scale = 2)
    private BigDecimal premiumFee = new BigDecimal("0");

    @Column(name = "user_notes", columnDefinition = "TEXT")
    private String userNotes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    // ─── NEW V1 FEATURE 4: Shareable Link ───────────────────────────────────
    @Column(name = "share_token", unique = true, length = 32)
    private String shareToken;

    @Column(name = "share_expires_at")
    private OffsetDateTime shareExpiresAt;
    // ─────────────────────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getClient() { return client; }
    public void setClient(User client) { this.client = client; }
    public Professional getProfessional() { return professional; }
    public void setProfessional(Professional professional) { this.professional = professional; }
    public Service getService() { return service; }
    public void setService(Service service) { this.service = service; }
    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }
    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public AppointmentPriority getPriority() { return priority; }
    public void setPriority(AppointmentPriority priority) { this.priority = priority; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getClientNotes() { return clientNotes; }
    public void setClientNotes(String clientNotes) { this.clientNotes = clientNotes; }
    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }
    public boolean isVirtual() { return virtual; }
    public void setVirtual(boolean virtual) { this.virtual = virtual; }
    public String getMeetLink() { return meetLink; }
    public void setMeetLink(String meetLink) { this.meetLink = meetLink; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public User getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(User cancelledBy) { this.cancelledBy = cancelledBy; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    public OffsetDateTime getShareExpiresAt() { return shareExpiresAt; }
    public void setShareExpiresAt(OffsetDateTime shareExpiresAt) { this.shareExpiresAt = shareExpiresAt; }
    public String getMeetingToken() { return meetingToken; }
    public void setMeetingToken(String meetingToken) { this.meetingToken = meetingToken; }
    public UUID getWorkflowInstanceId() { return workflowInstanceId; }
    public void setWorkflowInstanceId(UUID workflowInstanceId) { this.workflowInstanceId = workflowInstanceId; }
    public Double getClientLat() { return clientLat; }
    public void setClientLat(Double clientLat) { this.clientLat = clientLat; }
    public Double getClientLon() { return clientLon; }
    public void setClientLon(Double clientLon) { this.clientLon = clientLon; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
    public com.appointunified.enums.PaymentStatus getDepositStatus() { return depositStatus; }
    public void setDepositStatus(com.appointunified.enums.PaymentStatus depositStatus) { this.depositStatus = depositStatus; }
    public com.appointunified.enums.PaymentStatus getFinalPaymentStatus() { return finalPaymentStatus; }
    public void setFinalPaymentStatus(com.appointunified.enums.PaymentStatus finalPaymentStatus) { this.finalPaymentStatus = finalPaymentStatus; }
    public java.math.BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getPremiumFee() { return premiumFee; }
    public void setPremiumFee(BigDecimal premiumFee) { this.premiumFee = premiumFee; }
    public String getUserNotes() { return userNotes; }
    public void setUserNotes(String userNotes) { this.userNotes = userNotes; }
    public UUID getProfessionalId() { return professional != null ? professional.getId() : null; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
