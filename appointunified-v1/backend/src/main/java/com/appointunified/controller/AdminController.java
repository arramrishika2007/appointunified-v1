package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.entity.AdminAction;
import com.appointunified.entity.Professional;
import com.appointunified.enums.VerificationStatus;
import com.appointunified.exception.AppException;
import com.appointunified.repository.AdminActionRepository;
import com.appointunified.repository.VerificationDocumentRepository;
import com.appointunified.service.VerificationService;
import com.appointunified.repository.ProfessionalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin", description = "Admin-only: verifications, professional management")
public class AdminController {

    private static final String CLOUDINARY_SAMPLE_PATH = "/image/upload/v1312461204/sample.jpg";

    private final ProfessionalRepository professionalRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final VerificationService verificationService;
    private final AdminActionRepository adminActionRepository;

    // ─── Verification Queue ──────────────────────────────────────────────────

    @GetMapping("/verifications")
    @Operation(summary = "List professionals by verification status")
    public ResponseEntity<ApiResponse<Page<PendingProfessional>>> listVerifications(
            @RequestParam(defaultValue = "PENDING") String status,
            @PageableDefault(size = 20) Pageable pageable) {

        Pageable newestFirst = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        final VerificationStatus vs;
        try {
            vs = VerificationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw AppException.badRequest("Invalid status. Allowed values: PENDING, APPROVED, REJECTED, SUSPENDED");
        }

        Page<PendingProfessional> page = professionalRepository
            .findByVerificationStatus(vs, newestFirst)
            .map(p -> new PendingProfessional(
                p.getId(),
                p.getDisplayName(),
                p.getSector() != null ? p.getSector().name() : null,
                p.getSpecialty(),
                p.getLicenseNumber(),
                p.getVerificationStatus().name(),
                p.getUser() != null ? p.getUser().getPhone() : null,
                p.getUser() != null ? p.getUser().getEmail() : null,
                p.getCreatedAt()
            ));

        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PatchMapping("/verifications/{professionalId}/approve")
    @Operation(summary = "Approve a professional's verification")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable UUID professionalId,
            @AuthenticationPrincipal UUID adminId) {

        verificationService.approveProfessional(adminId, professionalId, "Approved from admin dashboard");

        return ResponseEntity.ok(ApiResponse.ok("Professional approved and can now accept bookings", null));
    }

    @GetMapping("/verifications/{professionalId}")
    @Operation(summary = "Get verification details and submitted documents for one professional")
    public ResponseEntity<ApiResponse<VerificationDetail>> getVerificationDetail(@PathVariable UUID professionalId) {
        Professional p = professionalRepository.findWithUserById(professionalId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));

        List<VerificationDocumentView> docs = verificationDocumentRepository
            .findByProfessionalIdOrderBySubmittedAtDesc(professionalId)
            .stream()
            .filter(d -> isPreviewableDocumentUrl(d.getDocUrl()))
            .map(d -> new VerificationDocumentView(
                d.getId(),
                d.getDocType(),
                d.getDocUrl(),
                d.getMimeType(),
                d.getStatus(),
                d.getSubmittedAt(),
                d.getReviewNotes(),
                d.getReviewedAt()
            ))
            .toList();

        VerificationDetail detail = new VerificationDetail(
            p.getId(),
            p.getDisplayName(),
            p.getSector() != null ? p.getSector().name() : null,
            p.getSpecialty(),
            p.getLicenseNumber(),
            p.getVerificationStatus().name(),
            p.getUser() != null ? p.getUser().getPhone() : null,
            p.getUser() != null ? p.getUser().getEmail() : null,
            p.getCreatedAt(),
            docs
        );

        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    private boolean isPreviewableDocumentUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String normalized = url.toLowerCase();
        return !normalized.contains(CLOUDINARY_SAMPLE_PATH) && !normalized.contains("sample.jpg?mock=");
    }

    @PatchMapping("/verifications/{professionalId}/reject")
    @Operation(summary = "Reject a professional's verification with reason")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable UUID professionalId,
            @Valid @RequestBody RejectRequest request,
            @AuthenticationPrincipal UUID adminId) {

        verificationService.rejectProfessional(adminId, professionalId, request.getReason());

        return ResponseEntity.ok(ApiResponse.ok("Professional rejected", null));
    }

    @GetMapping("/super-admin/verification-decisions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Super-admin decision audit grouped by sector")
    public ResponseEntity<ApiResponse<List<SectorDecisionSummary>>> verificationDecisionAudit(
            @RequestParam(defaultValue = "200") int size) {

        int cappedSize = Math.max(1, Math.min(size, 500));
        Pageable pageable = PageRequest.of(0, cappedSize);

        List<AdminAction> actions = adminActionRepository.findDecisionActions(
                List.of("APPROVE_PROFESSIONAL", "REJECT_PROFESSIONAL"),
                pageable);

        Map<String, List<DecisionAuditItem>> bySector = new LinkedHashMap<>();

        for (AdminAction action : actions) {
            Professional target = action.getTargetProfessional();
            if (target == null) {
                continue;
            }

            String sector = target.getSector() != null ? target.getSector().name() : "UNKNOWN";
            String decision = action.getActionType().contains("REJECT") ? "REJECTED" : "APPROVED";
            String adminName = action.getAdmin().getFullName();
            if (adminName == null || adminName.isBlank()) {
                adminName = action.getAdmin().getEmail() != null
                        ? action.getAdmin().getEmail()
                        : action.getAdmin().getPhone();
            }

            DecisionAuditItem item = new DecisionAuditItem(
                    target.getId(),
                    target.getDisplayName(),
                    decision,
                    action.getNotes(),
                    action.getCreatedAt(),
                    adminName,
                    action.getAdmin().getEmail(),
                    target.getSpecialty(),
                    target.getLicenseNumber());

            bySector.computeIfAbsent(sector, ignored -> new ArrayList<>()).add(item);
        }

        List<SectorDecisionSummary> response = bySector.entrySet().stream()
                .map(entry -> {
                    List<DecisionAuditItem> items = entry.getValue();
                    long approvedCount = items.stream().filter(i -> "APPROVED".equals(i.decision())).count();
                    long rejectedCount = items.stream().filter(i -> "REJECTED".equals(i.decision())).count();
                    return new SectorDecisionSummary(entry.getKey(), approvedCount, rejectedCount, items);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/professionals/{professionalId}/suspend")
    @Operation(summary = "Suspend a professional account")
    public ResponseEntity<ApiResponse<Void>> suspend(@PathVariable UUID professionalId) {
        Professional p = professionalRepository.findById(professionalId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));
        p.setVerificationStatus(VerificationStatus.SUSPENDED);
        p.setAcceptingBookings(false);
        professionalRepository.save(p);
        return ResponseEntity.ok(ApiResponse.ok("Professional suspended", null));
    }

    @PatchMapping("/professionals/{professionalId}/unsuspend")
    @Operation(summary = "Reinstate a suspended professional")
    public ResponseEntity<ApiResponse<Void>> unsuspend(@PathVariable UUID professionalId) {
        Professional p = professionalRepository.findById(professionalId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));
        p.setVerificationStatus(VerificationStatus.APPROVED);
        p.setAcceptingBookings(true);
        professionalRepository.save(p);
        return ResponseEntity.ok(ApiResponse.ok("Professional reinstated", null));
    }

    // ─── Inner DTOs ──────────────────────────────────────────────────────────

    public record PendingProfessional(
        UUID id,
        String displayName,
        String sector,
        String specialty,
        String licenseNumber,
        String verificationStatus,
        String phone,
        String email,
        OffsetDateTime submittedAt
    ) {}

    public record VerificationDocumentView(
        UUID id,
        String docType,
        String docUrl,
        String mimeType,
        String status,
        OffsetDateTime submittedAt,
        String reviewNotes,
        OffsetDateTime reviewedAt
    ) {}

    public record VerificationDetail(
        UUID id,
        String displayName,
        String sector,
        String specialty,
        String licenseNumber,
        String verificationStatus,
        String phone,
        String email,
        OffsetDateTime submittedAt,
        List<VerificationDocumentView> documents
    ) {}

    public record DecisionAuditItem(
        UUID professionalId,
        String professionalName,
        String decision,
        String reason,
        OffsetDateTime decidedAt,
        String adminName,
        String adminEmail,
        String specialty,
        String licenseNumber
    ) {}

    public record SectorDecisionSummary(
        String sector,
        long approvedCount,
        long rejectedCount,
        List<DecisionAuditItem> decisions
    ) {}

    @Data
    public static class RejectRequest {
        @NotBlank
        private String reason;
    }
}
