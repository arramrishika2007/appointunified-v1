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
}
