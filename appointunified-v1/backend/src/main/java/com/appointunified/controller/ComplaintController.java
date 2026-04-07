package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Complaints V2", description = "Complaint filing and admin resolution")
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping("/complaints")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "File a complaint against a professional")
    public ResponseEntity<ApiResponse<ComplaintService.ComplaintView>> fileComplaint(
            @AuthenticationPrincipal UUID userId,
            @RequestBody FileComplaintRequest request) {
        ComplaintService.FileComplaintRequest payload = new ComplaintService.FileComplaintRequest(
                request.professionalId(),
                request.appointmentId(),
                request.category(),
                request.description(),
                request.priority());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Complaint filed", complaintService.file(userId, payload)));
    }

    @GetMapping("/admin/complaints")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "List complaints by status")
    public ResponseEntity<ApiResponse<Page<ComplaintService.ComplaintView>>> listComplaints(
            @RequestParam(defaultValue = "OPEN") String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(complaintService.listByStatus(status, pageable)));
    }

    @PatchMapping("/admin/complaints/{complaintId}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Resolve complaint and optionally suspend professional")
    public ResponseEntity<ApiResponse<ComplaintService.ComplaintView>> resolveComplaint(
            @AuthenticationPrincipal UUID adminId,
            @PathVariable UUID complaintId,
            @RequestBody ResolveComplaintRequest request) {
        ComplaintService.ResolveComplaintRequest payload = new ComplaintService.ResolveComplaintRequest(
                request.resolutionNotes(),
                request.suspendProfessional());
        return ResponseEntity.ok(ApiResponse.ok(complaintService.resolve(adminId, complaintId, payload)));
    }

    @PatchMapping("/admin/complaints/{complaintId}/dismiss")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Dismiss complaint with reason")
    public ResponseEntity<ApiResponse<ComplaintService.ComplaintView>> dismissComplaint(
            @AuthenticationPrincipal UUID adminId,
            @PathVariable UUID complaintId,
            @RequestBody DismissComplaintRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                complaintService.dismiss(adminId, complaintId, request.reason())));
    }

    public record FileComplaintRequest(
            @NotNull UUID professionalId,
            UUID appointmentId,
            @NotBlank String category,
            @NotBlank String description,
            String priority
    ) {}

    public record ResolveComplaintRequest(@NotBlank String resolutionNotes, boolean suspendProfessional) {}

    public record DismissComplaintRequest(@NotBlank String reason) {}
}
