package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.dto.response.WaitlistResponse;
import com.appointunified.service.WaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/waitlist")
@Tag(name = "Waitlist", description = "V5 waitlist join/list/cancel")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping
    @Operation(summary = "Join waitlist for a professional/service")
    public ResponseEntity<ApiResponse<WaitlistResponse.Summary>> join(
            @AuthenticationPrincipal UUID userId,
            @RequestBody JoinWaitlistRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(waitlistService.join(
                userId,
                request.getProfessionalId(),
                request.getServiceId(),
                request.getPreferredTimeFrom(),
                request.getPreferredTimeTo()
        )));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my active waitlist entries")
    public ResponseEntity<ApiResponse<List<WaitlistResponse.Summary>>> getMine(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(waitlistService.getMyActive(userId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel my waitlist entry")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        waitlistService.cancel(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Waitlist entry removed", null));
    }

    public static class JoinWaitlistRequest {
        @NotNull
        private UUID professionalId;
        private UUID serviceId;
        private LocalTime preferredTimeFrom;
        private LocalTime preferredTimeTo;

        public UUID getProfessionalId() { return professionalId; }
        public void setProfessionalId(UUID professionalId) { this.professionalId = professionalId; }
        public UUID getServiceId() { return serviceId; }
        public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
        public LocalTime getPreferredTimeFrom() { return preferredTimeFrom; }
        public void setPreferredTimeFrom(LocalTime preferredTimeFrom) { this.preferredTimeFrom = preferredTimeFrom; }
        public LocalTime getPreferredTimeTo() { return preferredTimeTo; }
        public void setPreferredTimeTo(LocalTime preferredTimeTo) { this.preferredTimeTo = preferredTimeTo; }
    }
}
