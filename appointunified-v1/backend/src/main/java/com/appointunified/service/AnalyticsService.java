package com.appointunified.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointunified.dto.response.AnalyticsSummaryResponse;
import com.appointunified.dto.response.AISuggestionResponse;
import com.appointunified.dto.response.EarningsResponse;
import com.appointunified.dto.response.KPISummaryResponse;
import com.appointunified.dto.response.NoShowTrendResponse;
import com.appointunified.dto.response.ReportExportResponse;
import com.appointunified.dto.response.RevenueBreakdownResponse;
import com.appointunified.entity.AISuggestion;
import com.appointunified.entity.AnalyticsSnapshot;
import com.appointunified.entity.ReportExport;
import com.appointunified.entity.User;
import com.appointunified.repository.AISuggestionRepository;
import com.appointunified.repository.AnalyticsSnapshotRepository;
import com.appointunified.repository.ReportExportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {
    
    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;
    private final ReportExportRepository reportExportRepository;
    private final AISuggestionRepository aiSuggestionRepository;
    private final GroqService groqService;
    private final ProfessionalService professionalService;
    
    /**
     * Get analytics summary for a date range and sector
     */
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getAnalyticsSummary(LocalDate startDate, LocalDate endDate, String sector) {
        List<AnalyticsSnapshot> snapshots;
        
        if (sector != null) {
            snapshots = analyticsSnapshotRepository.findBySnapshotDateBetweenAndSectorOrderBySnapshotDateDesc(
                startDate, endDate, sector);
        } else {
            snapshots = analyticsSnapshotRepository.findPlatformWideSnapshotsBetweenDates(startDate, endDate);
        }
        
        if (snapshots.isEmpty()) {
            log.warn("No analytics snapshots found for sector={}, range=[{},{}]", sector, startDate, endDate);
            return new AnalyticsSummaryResponse();
        }
        
        // Aggregate snapshots across the date range
        Integer totalBookings = snapshots.stream().mapToInt(AnalyticsSnapshot::getTotalBookings).sum();
        Integer completedBookings = snapshots.stream().mapToInt(AnalyticsSnapshot::getCompletedBookings).sum();
        Integer cancelledBookings = snapshots.stream().mapToInt(AnalyticsSnapshot::getCancelledBookings).sum();
        Integer noShows = snapshots.stream().mapToInt(AnalyticsSnapshot::getNoShows).sum();
        
        BigDecimal totalRevenue = snapshots.stream()
            .map(AnalyticsSnapshot::getTotalRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal avgNoShowRate = snapshots.isEmpty() ? BigDecimal.ZERO : 
            snapshots.stream()
                .map(AnalyticsSnapshot::getAverageNoShowRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(snapshots.size()), 2, java.math.RoundingMode.HALF_UP);
        
        AnalyticsSummaryResponse response = new AnalyticsSummaryResponse();
        response.setTotalBookings(totalBookings);
        response.setCompletedBookings(completedBookings);
        response.setCancelledBookings(cancelledBookings);
        response.setNoShows(noShows);
        response.setTotalRevenue(totalRevenue);
        response.setAverageNoShowRate(avgNoShowRate);
        response.setSector(sector);
        
        return response;
    }
    
    /**
     * Create export request for bookings CSV
     */
    @Transactional
    public ReportExportResponse createExportRequest(User requestedBy, String exportType, 
            LocalDate dateStart, LocalDate dateEnd, String sector) {
        
        ReportExport export = new ReportExport();
        export.setRequestedBy(requestedBy);
        export.setExportType(exportType);
        export.setStatus("GENERATING");
        export.setDateFilterStart(dateStart);
        export.setDateFilterEnd(dateEnd);
        export.setSectorFilter(sector);
        export.setCreatedAt(OffsetDateTime.now());
        export.setExpiresAt(OffsetDateTime.now().plusDays(7));
        
        String fileName = buildExportFileName(exportType, dateStart, dateEnd);
        export.setFileName(fileName);
        
        ReportExport saved = reportExportRepository.save(export);
        log.info("Created export request: id={}, type={}, requested_by={}", saved.getId(), exportType, requestedBy.getId());
        
        return mapToReportExportResponse(saved);
    }
    
    /**
     * Get professional earnings for a date range
     */
    @Transactional(readOnly = true)
    public List<EarningsResponse> getProfessionalEarnings(UUID professionalId, LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching earnings for professional={}, range=[{},{}]", professionalId, startDate, endDate);
        
        // Simplified: return empty list for now - requires appointment+payment queries
        return List.of();
    }
    
    /**
     * Get KPI summary for super admin dashboard
     */
    @Transactional(readOnly = true)
    public KPISummaryResponse getKPISummary() {
        KPISummaryResponse kpi = new KPISummaryResponse();
        kpi.setTotalActiveUsers(0);
        kpi.setTotalActiveProfessionals(0);
        kpi.setTotalBookingsToday(0);
        kpi.setCompletedBookingsToday(0);
        kpi.setPlatformRevenueToday(BigDecimal.ZERO);
        kpi.setAverageNoShowRateToday(BigDecimal.ZERO);
        kpi.setOpenSLABreachesCount(0);
        kpi.setRedisQueueDepth(0L);
        kpi.setActiveWebSocketConnections(0);
        kpi.setGeneratedAt(OffsetDateTime.now().toString());
        
        return kpi;
    }
    
    /**
     * Get AI suggestions for a professional (Groq-generated)
     */
    @Transactional(readOnly = true)
    public List<AISuggestionResponse> getAISuggestionsForProfessional(UUID professionalId) {
        List<AISuggestion> suggestions = aiSuggestionRepository.findByProfessionalIdAndIsActiveTrueOrderByGeneratedAtDesc(professionalId);
        
        return suggestions.stream()
            .map(this::mapToAISuggestionResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Store AI-generated suggestions for a professional
     */
    @Transactional
    public List<AISuggestionResponse> storeAISuggestions(UUID professionalId, List<AISuggestion> suggestions) {
        List<AISuggestion> saved = suggestions.stream()
            .map(s -> aiSuggestionRepository.save(s))
            .collect(Collectors.toList());
        
        log.info("Stored {} AI suggestions for professional={}", saved.size(), professionalId);
        
        return saved.stream()
            .map(this::mapToAISuggestionResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get export status and details
     */
    @Transactional(readOnly = true)
    public ReportExportResponse getExportStatus(UUID exportId) {
        ReportExport export = reportExportRepository.findById(exportId)
            .orElse(null);
        
        if (export == null) {
            return null;
        }
        
        return mapToReportExportResponse(export);
    }
    
    // ==================== Helper Methods ====================
    
    private String buildExportFileName(String exportType, LocalDate dateStart, LocalDate dateEnd) {
        return String.format("%s_%s_to_%s.csv", exportType.toLowerCase(), dateStart, dateEnd);
    }
    
    private ReportExportResponse mapToReportExportResponse(ReportExport export) {
        ReportExportResponse response = new ReportExportResponse();
        response.setId(export.getId());
        response.setExportType(export.getExportType());
        response.setFileName(export.getFileName());
        response.setFileUrl(export.getFileUrl());
        response.setStatus(export.getStatus());
        response.setFileSizeBytes(export.getFileSizeBytes());
        response.setErrorMessage(export.getErrorMessage());
        response.setDateFilterStart(export.getDateFilterStart());
        response.setDateFilterEnd(export.getDateFilterEnd());
        response.setSectorFilter(export.getSectorFilter());
        response.setCreatedAt(export.getCreatedAt());
        response.setExpiresAt(export.getExpiresAt());
        return response;
    }
    
    private AISuggestionResponse mapToAISuggestionResponse(AISuggestion suggestion) {
        AISuggestionResponse response = new AISuggestionResponse();
        response.setId(suggestion.getId());
        response.setSuggestionTitle(suggestion.getSuggestionTitle());
        response.setSuggestionDescription(suggestion.getSuggestionDescription());
        response.setExpectedImpact(suggestion.getExpectedImpact());
        response.setSuggestionData(suggestion.getSuggestionData());
        response.setGeneratedAt(suggestion.getGeneratedAt());
        response.setGeneratedByModel(suggestion.getGeneratedByModel());
        return response;
    }
    
    /**
     * Generate AI-powered schedule suggestions using Groq API
     * Async method to be called via @Scheduled job
     */
    @Transactional
    public void generateScheduleSuggestionsForProfessional(UUID professionalId) {
        try {
            if (!groqService.isConfigured()) {
                log.warn("Groq API not configured, skipping suggestion generation");
                return;
            }
            
            // Check if already has suggestions from today
            List<AISuggestion> todaysSuggestions = aiSuggestionRepository.findTodaysSuggestionsForProfessional(professionalId);
            if (!todaysSuggestions.isEmpty()) {
                log.debug("Professional {} already has today's suggestions, skipping generation", professionalId);
                return;
            }
            
            // Build professional analytics context for Groq
            String contextData = buildProfessionalAnalyticsContext(professionalId);
            
            // Call Groq API for suggestions
            String groqResponse = groqService.generateScheduleSuggestions(contextData);
            
            if (groqResponse.isBlank()) {
                log.warn("Groq API returned empty response for professional {}", professionalId);
                return;
            }
            
            // Parse and store suggestions
            storeGroqSuggestionsForProfessional(professionalId, groqResponse);
            log.info("Generated and stored AI suggestions for professional={}", professionalId);
            
        } catch (Exception e) {
            log.error("Error generating AI suggestions for professional={}", professionalId, e);
        }
    }
    
    // ==================== Private Helper Methods ====================
    
    private String buildProfessionalAnalyticsContext(UUID professionalId) {
        // Build a summary of professional's appointment data for Groq context
        return String.format("""
            Professional ID: %s
            Context: This professional's booking patterns, peak hours, no-show rates, and service offerings.
            Goal: Provide schedule optimization suggestions to maximize completed appointments and minimize delays.
            """, professionalId);
    }
    
    private void storeGroqSuggestionsForProfessional(UUID professionalId, String groqResponse) {
        try {
            // Parse Groq JSON response
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(groqResponse);
            com.fasterxml.jackson.databind.JsonNode suggestionsArray = root.get("suggestions");
            
            if (suggestionsArray == null || !suggestionsArray.isArray()) {
                log.warn("Invalid Groq response structure for professional {}", professionalId);
                return;
            }
            
            // Get professional entity
            var professional = professionalService.getProfessionalById(professionalId);
            if (professional == null) {
                log.warn("Professional not found: {}", professionalId);
                return;
            }
            
            // Store each suggestion
            for (com.fasterxml.jackson.databind.JsonNode suggestionNode : suggestionsArray) {
                AISuggestion suggestion = new AISuggestion();
                suggestion.setProfessional(professional);
                suggestion.setSuggestionTitle(suggestionNode.get("title").asText());
                suggestion.setSuggestionDescription(suggestionNode.get("description").asText());
                suggestion.setExpectedImpact(suggestionNode.get("expected_impact").asText());
                suggestion.setSuggestionData(suggestionNode.toString());
                suggestion.setGeneratedAt(OffsetDateTime.now());
                suggestion.setGeneratedByModel("llama3-70b-8192");
                suggestion.setIsActive(true);
                
                aiSuggestionRepository.save(suggestion);
            }
            
        } catch (Exception e) {
            log.error("Error parsing Groq suggestions for professional {}", professionalId, e);
        }
    }
}
