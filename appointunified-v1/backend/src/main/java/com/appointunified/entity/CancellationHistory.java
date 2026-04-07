package com.appointunified.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cancellation_history")
public class CancellationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "cancelled_at", nullable = false)
    private OffsetDateTime cancelledAt = OffsetDateTime.now();

    @Column(name = "hours_before_appointment", precision = 6, scale = 2)
    private BigDecimal hoursBeforeAppointment;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "penalty_applied", nullable = false, precision = 4, scale = 2)
    private BigDecimal penaltyApplied = BigDecimal.ZERO;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public BigDecimal getHoursBeforeAppointment() { return hoursBeforeAppointment; }
    public void setHoursBeforeAppointment(BigDecimal hoursBeforeAppointment) { this.hoursBeforeAppointment = hoursBeforeAppointment; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getPenaltyApplied() { return penaltyApplied; }
    public void setPenaltyApplied(BigDecimal penaltyApplied) { this.penaltyApplied = penaltyApplied; }
}
