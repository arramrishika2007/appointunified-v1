package com.appointunified.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appointunified.dto.response.AnalyticsSummaryResponse;
import com.appointunified.dto.response.AISuggestionResponse;
import com.appointunified.dto.response.EarningsResponse;
import com.appointunified.dto.response.KPISummaryResponse;
import com.appointunified.dto.response.NoShowTrendResponse;
import com.appointunified.dto.response.ReportExportResponse;
import com.appointunified.service.AnalyticsService;
import com.appointunified.security.AuthenticationHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/analytics")
@Slf4j
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    private final AuthenticationHelper authenticationHelper;
    
    /**
     * GET /api/analytics/summary
     * Platform-wide or sector-specific analytics summary
     * Auth: ADMIN/SUPER_ADMIN
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AnalyticsSummaryResponse> getAnalyticsSummary(
            @RequestParam(required = false) String sector,
            @RequestParam(defaultValue = "7d") String range) {
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(range);
        
        AnalyticsSummaryResponse summary = analyticsService.getAnalyticsSummary(startDate, endDate, sector);
        log.info("Retrieved analytics summary: sector={}, range={}", sector, range);
        
        return ResponseEntity.ok(summary);
    }
    
    /**
     * GET /api/analytics/no-show-trends
     * No-show rate trends for professionals in a date range
     * Auth: ADMIN
     */
    @GetMapping("/no-show-trends")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NoShowTrendResponse>> getNoShowTrends(
            @RequestParam(required = false) UUID professionalId,
            @RequestParam(required = false) String sector,
            @RequestParam(defaultValue = "30d") String range) {
        
        log.info("Retrieved no-show trends: professionalId={}, sector={}, range={}", professionalId, sector, range);
        
        // Simplified: return empty list for now - requires appointment query
        return ResponseEntity.ok(List.of());
    }
    
    /**
     * GET /api/analytics/peak-hours
     * Peak booking hours heatmap data
     * Auth: ADMIN
     */
    @GetMapping("/peak-hours")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPeakHoursData(
            @RequestParam(required = false) String sector,
            @RequestParam(defaultValue = "30d") String range) {
        
        log.info("Retrieved peak hours data: sector={}, range={}", sector, range);
        
        // Simplified: return empty array for now - requires time-series analysis
        return ResponseEntity.ok(List.of());
    }
    
    /**
     * GET /api/analytics/revenue
     * Revenue breakdown by service type
     * Auth: ADMIN/PROFESSIONAL
     */
    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSIONAL','SUPER_ADMIN')")
    public ResponseEntity<List<RevenueBreakdownResponse>> getRevenueBreakdown(
            @RequestParam(required = false) String sector,
            @RequestParam(defaultValue = "30d") String range) {
        
        log.info("Retrieved revenue breakdown: sector={}, range={}", sector, range);
        
        // Simplified: return empty list for now - requires payment_records query
        return ResponseEntity.ok(List.of());
    }
    
    /**
     * GET /api/analytics/slot-suggestions/{professionalId}
     * AI-powered schedule optimization suggestions (Groq-generated)
     * Auth: PROFESSIONAL (self only) / ADMIN
     */
    @GetMapping("/slot-suggestions/{professionalId}")
    @PreAuthorize("hasAnyRole('ADMIN','PROFESSIONAL')")
    public ResponseEntity<List<AISuggestionResponse>> getSlotSuggestions(
            @PathVariable UUID professionalId) {
        
        // Verify professional accessing their own suggestions
        authenticationHelper.verifyProfessionalAccess(professionalId);
        
        List<AISuggestionResponse> suggestions = analyticsService.getAISuggestionsForProfessional(professionalId);
        log.info("Retrieved AI suggestions for professional={}", professionalId);
        
        return ResponseEntity.ok(suggestions);
    }
    
    /**
     * GET /api/analytics/kpi-summary
     * Super admin KPI dashboard data
     * Auth: SUPER_ADMIN
     */
    @GetMapping("/kpi-summary")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<KPISummaryResponse> getKPISummary() {
        KPISummaryResponse kpi = analyticsService.getKPISummary();
        log.info("Retrieved KPI summary for super admin dashboard");
        
        return ResponseEntity.ok(kpi);
    }
    
    /**
     * POST /api/exports/bookings
     * Request CSV export of bookings
     * Auth: ADMIN
     */
    @PostMapping("/exports/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportExportResponse> exportBookings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd,
            @RequestParam(required = false) String sector) {
        
        var currentUser = authenticationHelper.getCurrentUser();
        ReportExportResponse response = analyticsService.createExportRequest(
            currentUser, "BOOKINGS", dateStart, dateEnd, sector);
        
        log.info("Created bookings export request: {}", response.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    /**
     * POST /api/exports/audit-log
     * Request CSV export of audit logs
     * Auth: SUPER_ADMIN
     */
    @PostMapping("/exports/audit-log")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ReportExportResponse> exportAuditLog(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd) {
        
        var currentUser = authenticationHelper.getCurrentUser();
        ReportExportResponse response = analyticsService.createExportRequest(
            currentUser, "AUDIT_LOG", dateStart, dateEnd, null);
        
        log.info("Created audit log export request: {}", response.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    /**
     * GET /api/exports/{exportId}/status
     * Check status of an export request
     * Auth: Any authenticated user (owner of export)
     */
    @GetMapping("/exports/{exportId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReportExportResponse> getExportStatus(
            @PathVariable UUID exportId) {
        
        ReportExportResponse response = analyticsService.getExportStatus(exportId);
        
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        
        log.info("Retrieved export status: id={}, status={}", exportId, response.getStatus());
        return ResponseEntity.ok(response);
    }
    
    // ==================== Helper Methods ====================
    
    private LocalDate calculateStartDate(String range) {
        LocalDate endDate = LocalDate.now();
        return switch (range) {
            case "7d" -> endDate.minusDays(7);
            case "30d" -> endDate.minusDays(30);
            case "90d" -> endDate.minusDays(90);
            default -> endDate.minusDays(7);
        };
    }
}
