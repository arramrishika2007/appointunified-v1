package com.appointunified.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "report_exports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportExport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;
    
    @Column(name = "export_type", nullable = false)
    private String exportType;  // 'BOOKINGS','NO_SHOW_REPORT','AUDIT_LOG','VERIFICATION_REPORT'
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_url")
    private String fileUrl;
    
    @Column(name = "file_hash")
    private String fileHash;
    
    @Column(name = "status", nullable = false)
    private String status = "GENERATING";  // 'GENERATING','READY','FAILED','EXPIRED'
    
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "date_filter_start")
    private LocalDate dateFilterStart;
    
    @Column(name = "date_filter_end")
    private LocalDate dateFilterEnd;
    
    @Column(name = "sector_filter")
    private String sectorFilter;
    
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private OffsetDateTime createdAt;
    
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
}
