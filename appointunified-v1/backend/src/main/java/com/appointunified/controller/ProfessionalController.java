package com.appointunified.controller;

import com.appointunified.dto.request.ProfessionalRequest;
import com.appointunified.dto.response.ApiResponse;
import com.appointunified.dto.response.AppointmentResponse;
import com.appointunified.dto.response.ProfessionalResponse;
import com.appointunified.service.ProfessionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
@Tag(name = "Professionals", description = "Professional profiles and availability")
public class ProfessionalController {

    private final ProfessionalService professionalService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Register professional profile")
    public ResponseEntity<ApiResponse<ProfessionalResponse.Detail>> register(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ProfessionalRequest.Register request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Professional profile created. Pending verification.",
                        professionalService.register(userId, request)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Get own professional profile")
    public ResponseEntity<ApiResponse<ProfessionalResponse.Detail>> getMyProfile(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(professionalService.getMyProfile(userId)));
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Update own professional profile")
    public ResponseEntity<ApiResponse<ProfessionalResponse.Detail>> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ProfessionalRequest.Update request) {
        return ResponseEntity.ok(ApiResponse.ok(professionalService.updateProfile(userId, request)));
    }

    // NEW V1 FEATURE 2: Update mood status
    @PatchMapping("/me/mood")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Update availability mood/status")
    public ResponseEntity<ApiResponse<ProfessionalResponse.Summary>> updateMood(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ProfessionalRequest.MoodUpdate request) {
        return ResponseEntity.ok(ApiResponse.ok(professionalService.updateMood(userId, request)));
    }

    @GetMapping
    @Operation(summary = "Search verified professionals")
    public ResponseEntity<ApiResponse<Page<ProfessionalResponse.Summary>>> search(
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 12, sort = "ratingAvg") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                professionalService.searchVerified(sector, city, query, pageable)));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get nearby professionals within radius")
    public ResponseEntity<ApiResponse<List<ProfessionalResponse.Summary>>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "20") double radiusKm,
            @RequestParam(required = false) String sector,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(professionalService.searchNearby(lat, lng, radiusKm, sector, limit)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get professional profile by ID")
    public ResponseEntity<ApiResponse<ProfessionalResponse.Detail>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(professionalService.getById(id)));
    }

    @GetMapping("/{id}/slots")
    @Operation(summary = "Get available booking slots for a date")
    public ResponseEntity<ApiResponse<List<AppointmentResponse.AvailableSlot>>> getSlots(
            @PathVariable UUID id,
            @RequestParam LocalDate date,
            @RequestParam(required = false) UUID serviceId) {
        return ResponseEntity.ok(ApiResponse.ok(
                professionalService.getAvailableSlots(id, date, serviceId)));
    }

    @PatchMapping("/{id}/overbooking")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Toggle smart overbooking for own profile")
    public ResponseEntity<ApiResponse<ProfessionalResponse.Summary>> updateOverbooking(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @RequestBody OverbookingRequest request) {
        boolean enabled = request != null && request.getAllowOverbooking() != null && request.getAllowOverbooking();
        return ResponseEntity.ok(ApiResponse.ok(professionalService.updateOverbooking(userId, id, enabled)));
    }

    @PatchMapping("/{id}/service-area")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Update professional service area radius and center")
    public ResponseEntity<ApiResponse<ProfessionalResponse.Summary>> updateServiceArea(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody ServiceAreaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                professionalService.updateServiceArea(userId, id, request.getRadiusKm(), request.getCenterLat(), request.getCenterLng())
        ));
    }

    @PatchMapping("/me/service-area")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Update own service area")
    public ResponseEntity<ApiResponse<ProfessionalResponse.Summary>> updateMyServiceArea(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ServiceAreaRequest request) {
        ProfessionalResponse.Detail me = professionalService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(
                professionalService.updateServiceArea(userId, me.getId(), request.getRadiusKm(), request.getCenterLat(), request.getCenterLng())
        ));
    }

    @PatchMapping("/me/overbooking")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Toggle smart overbooking for own profile (me)")
    public ResponseEntity<ApiResponse<ProfessionalResponse.Summary>> updateMyOverbooking(
            @AuthenticationPrincipal UUID userId,
            @RequestBody OverbookingRequest request) {
        ProfessionalResponse.Detail me = professionalService.getMyProfile(userId);
        boolean enabled = request != null && request.getAllowOverbooking() != null && request.getAllowOverbooking();
        return ResponseEntity.ok(ApiResponse.ok(professionalService.updateOverbooking(userId, me.getId(), enabled)));
    }

    public static class OverbookingRequest {
        private Boolean allowOverbooking;

        public Boolean getAllowOverbooking() { return allowOverbooking; }
        public void setAllowOverbooking(Boolean allowOverbooking) { this.allowOverbooking = allowOverbooking; }
    }

    public static class ServiceAreaRequest {
        private java.math.BigDecimal radiusKm;
        private java.math.BigDecimal centerLat;
        private java.math.BigDecimal centerLng;

        public java.math.BigDecimal getRadiusKm() { return radiusKm; }
        public void setRadiusKm(java.math.BigDecimal radiusKm) { this.radiusKm = radiusKm; }
        public java.math.BigDecimal getCenterLat() { return centerLat; }
        public void setCenterLat(java.math.BigDecimal centerLat) { this.centerLat = centerLat; }
        public java.math.BigDecimal getCenterLng() { return centerLng; }
        public void setCenterLng(java.math.BigDecimal centerLng) { this.centerLng = centerLng; }
    }
}
