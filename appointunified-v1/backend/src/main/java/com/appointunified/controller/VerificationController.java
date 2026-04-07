package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Verification V2", description = "Professional document verification and admin review")
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping("/professional/verification/status")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Get professional verification status and submitted documents")
    public ResponseEntity<ApiResponse<VerificationService.VerificationStatusView>> getStatus(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(verificationService.getStatus(userId)));
    }

    @PostMapping("/professional/verification/documents")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Submit a verification document")
    public ResponseEntity<ApiResponse<VerificationService.DocumentView>> submitDocument(
            @AuthenticationPrincipal UUID userId,
            @RequestBody SubmitDocumentRequest request) {
        VerificationService.SubmitDocumentRequest payload = new VerificationService.SubmitDocumentRequest(
                request.docType(),
                request.docUrl(),
                request.docHash(),
                request.fileSizeBytes(),
                request.mimeType());
        return ResponseEntity.ok(ApiResponse.ok(
                "Document submitted for review",
                verificationService.submitDocument(userId, payload)));
    }



    @GetMapping("/admin/verification/pending")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "List pending verification documents")
    public ResponseEntity<ApiResponse<Page<VerificationService.PendingDocumentView>>> getPending(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(verificationService.getPending(pageable)));
    }

    @PatchMapping("/admin/verification/documents/{docId}/review")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Approve or reject one document")
    public ResponseEntity<ApiResponse<VerificationService.DocumentView>> reviewDocument(
            @AuthenticationPrincipal UUID adminId,
            @PathVariable UUID docId,
            @RequestBody ReviewDocumentRequest request) {
        VerificationService.ReviewDocumentRequest payload = new VerificationService.ReviewDocumentRequest(
                request.decision(),
                request.notes());
        return ResponseEntity.ok(ApiResponse.ok(verificationService.reviewDocument(adminId, docId, payload)));
    }

    @PatchMapping("/admin/verification/professionals/{professionalId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Approve a professional and activate booking")
    public ResponseEntity<ApiResponse<Void>> approveProfessional(
            @AuthenticationPrincipal UUID adminId,
            @PathVariable UUID professionalId,
            @Valid @RequestBody ApproveProfessionalRequest request) {
        verificationService.approveProfessional(adminId, professionalId, request.notes());
        return ResponseEntity.ok(ApiResponse.ok("Professional approved", null));
    }

    @PatchMapping("/admin/verification/professionals/{professionalId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Reject a professional")
    public ResponseEntity<ApiResponse<Void>> rejectProfessional(
            @AuthenticationPrincipal UUID adminId,
            @PathVariable UUID professionalId,
            @Valid @RequestBody RejectProfessionalRequest request) {
        verificationService.rejectProfessional(adminId, professionalId, request.notes());
        return ResponseEntity.ok(ApiResponse.ok("Professional rejected", null));
    }

    public record SubmitDocumentRequest(
            @NotBlank String docType,
            @NotBlank String docUrl,
            String docHash,
            Integer fileSizeBytes,
            String mimeType
    ) {}

    public record ReviewDocumentRequest(@NotBlank String decision, String notes) {}

    public record ApproveProfessionalRequest(@NotBlank(message = "Approval reason is required") String notes) {}
    public record RejectProfessionalRequest(@NotBlank(message = "Rejection reason is required") String notes) {}
}
