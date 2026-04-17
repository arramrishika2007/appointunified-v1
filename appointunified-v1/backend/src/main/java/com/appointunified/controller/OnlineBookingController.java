package com.appointunified.controller;

import com.appointunified.dto.request.AppointmentRequest;
import com.appointunified.dto.response.AppointmentResponse;
import com.appointunified.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/online-bookings")
@RequiredArgsConstructor
public class OnlineBookingController {

    private final AppointmentService appointmentService;

    @PostMapping("/lock-slot")
    public ResponseEntity<AppointmentResponse.Summary> lockSlot(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid AppointmentRequest.Create request) {
        return ResponseEntity.ok(appointmentService.lockSlot(userId, request));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<AppointmentResponse.Summary> confirmBooking(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        // Confirm deposit and actually book
        return ResponseEntity.ok(appointmentService.confirmDepositAndBook(id, userId));
    }
    
    @PostMapping("/{id}/complete")
    public ResponseEntity<AppointmentResponse.Summary> completeBooking(
            @AuthenticationPrincipal UUID professionalUserId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.markComplete(id, professionalUserId));
    }
}
