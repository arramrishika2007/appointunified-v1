package com.appointunified.controller;

import com.appointunified.dto.request.AppointmentRequest;
import com.appointunified.dto.response.ApiResponse;
import com.appointunified.dto.response.AppointmentResponse;
import com.appointunified.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Booking, cancellation, rescheduling, drafts")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Create a new appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse.Summary>> create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody AppointmentRequest.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Appointment booked successfully",
                        appointmentService.create(userId, request)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my appointments")
    public ResponseEntity<ApiResponse<Page<AppointmentResponse.Summary>>> getMyAppointments(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.getMyAppointments(userId, pageable)));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse.Summary>> cancel(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @RequestBody(required = false) AppointmentRequest.Cancel request) {
        if (request == null) request = new AppointmentRequest.Cancel();
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.cancel(id, userId, request)));
    }

    @PutMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule an appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse.Summary>> reschedule(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody AppointmentRequest.Reschedule request) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.reschedule(id, userId, request)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Mark appointment as completed")
    public ResponseEntity<ApiResponse<AppointmentResponse.Summary>> markComplete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.markComplete(id, userId)));
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Mark appointment as no-show")
    public ResponseEntity<ApiResponse<AppointmentResponse.Summary>> markNoShow(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.markNoShow(id, userId)));
    }

    // NEW V1 FEATURE 5: Confirm Deposit Payment
    @PostMapping("/{id}/confirm-deposit")
    @Operation(summary = "Confirm that the user has paid the deposit (honesty system button)")
    public ResponseEntity<ApiResponse<AppointmentResponse.Summary>> confirmDepositAndBook(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.confirmDepositAndBook(id, userId)));
    }

    // NEW V1 FEATURE 5: Professional verified full/final payment
    @PostMapping("/{id}/verify-final-payment")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Professional verifies final payment received")
    public ResponseEntity<ApiResponse<AppointmentResponse.Summary>> verifyFinalPayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.verifyFinalPayment(id, userId)));
    }

    // NEW V1 FEATURE 4: Share link
    @GetMapping("/{id}/share")
    @Operation(summary = "Get shareable link for appointment")
    public ResponseEntity<ApiResponse<AppointmentResponse.ShareInfo>> getShareInfo(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.getShareInfo(id, userId)));
    }

    // NEW V1 FEATURE 4: iCal download (public)
    @GetMapping("/{id}/ical")
    @Operation(summary = "Download iCal file for appointment")
    public ResponseEntity<byte[]> downloadIcal(@PathVariable UUID id) {
        String ical = appointmentService.generateIcal(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("appointment-" + id + ".ics").build());
        return ResponseEntity.ok().headers(headers).body(ical.getBytes());
    }

    // Public shared appointment view
    @GetMapping("/share/{shareToken}")
    @Operation(summary = "View shared appointment (no auth needed)")
    public ResponseEntity<ApiResponse<AppointmentResponse.Summary>> getByShareToken(
            @PathVariable String shareToken) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.getByShareToken(shareToken)));
    }

    @GetMapping("/meeting/validate-token/{meetingToken}")
    @Operation(summary = "Validate meeting token and check join eligibility")
    public ResponseEntity<ApiResponse<AppointmentResponse.MeetingJoinInfo>> validateMeetingToken(
            @PathVariable String meetingToken) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.validateMeetingToken(meetingToken)));
    }

    // NEW V1 FEATURE 3: Draft endpoints
    @PostMapping("/drafts")
    @Operation(summary = "Save booking draft (resume later)")
    public ResponseEntity<ApiResponse<AppointmentResponse.DraftSummary>> saveDraft(
            @AuthenticationPrincipal UUID userId,
            @RequestBody AppointmentRequest.SaveDraft request) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.saveDraft(userId, request)));
    }

    @GetMapping("/drafts")
    @Operation(summary = "Get my saved booking drafts")
    public ResponseEntity<ApiResponse<List<AppointmentResponse.DraftSummary>>> getMyDrafts(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(appointmentService.getMyDrafts(userId)));
    }

    @DeleteMapping("/drafts/{draftId}")
    @Operation(summary = "Delete a booking draft")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(
            @PathVariable UUID draftId,
            @AuthenticationPrincipal UUID userId) {
        appointmentService.deleteDraft(draftId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Draft deleted", null));
    }
}
