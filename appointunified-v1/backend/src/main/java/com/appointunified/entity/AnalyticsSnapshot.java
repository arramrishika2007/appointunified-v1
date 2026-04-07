package com.appointunified.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "analytics_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
    
    @Column(name = "sector")
    private String sector;  // 'HEALTHCARE','GOVERNMENT','SERVICES' or null for platform-wide
    
    @Column(name = "total_bookings", nullable = false)
    private Integer totalBookings = 0;
    
    @Column(name = "completed_bookings", nullable = false)
    private Integer completedBookings = 0;
    
    @Column(name = "cancelled_bookings", nullable = false)
    private Integer cancelledBookings = 0;
    
    @Column(name = "no_shows", nullable = false)
    private Integer noShows = 0;
    
    @Column(name = "total_revenue", nullable = false)
    private BigDecimal totalRevenue = new BigDecimal("0.00");
    
    @Column(name = "new_users", nullable = false)
    private Integer newUsers = 0;
    
    @Column(name = "new_professionals", nullable = false)
    private Integer newProfessionals = 0;
    
    @Column(name = "average_no_show_rate", nullable = false)
    private BigDecimal averageNoShowRate = new BigDecimal("0.00");
    
    @Column(name = "average_completion_rate", nullable = false)
    private BigDecimal averageCompletionRate = new BigDecimal("0.00");
    
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private OffsetDateTime createdAt;
}
