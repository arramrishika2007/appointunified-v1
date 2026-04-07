package com.appointunified.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportExportResponse {
    private UUID id;
    private String exportType;  // 'BOOKINGS','NO_SHOW_REPORT','AUDIT_LOG','VERIFICATION_REPORT'
    private String fileName;
    private String fileUrl;
    private String status;  // 'GENERATING','READY','FAILED','EXPIRED'
    private Long fileSizeBytes;
    private String errorMessage;
    private LocalDate dateFilterStart;
    private LocalDate dateFilterEnd;
    private String sectorFilter;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
}
