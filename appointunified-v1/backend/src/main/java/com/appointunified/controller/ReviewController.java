package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.entity.*;
import com.appointunified.exception.AppException;
import com.appointunified.repository.*;
import com.appointunified.enums.AppointmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Post-appointment ratings and reviews")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;

    /** List reviews for a professional (public) */
    @GetMapping("/professional/{professionalId}")
    @Operation(summary = "Get reviews for a professional")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> listForProfessional(
            @PathVariable UUID professionalId,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<ReviewResponse> page = reviewRepository
            .findByProfessionalIdAndVisibleTrueOrderByCreatedAtDesc(professionalId, pageable)
            .map(this::toResponse);

        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /** List reviews submitted by the current user */
    @GetMapping("/me")
    @Operation(summary = "Get reviews submitted by me")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> listMine(
            @AuthenticationPrincipal UUID userId,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<ReviewResponse> page = reviewRepository
            .findByReviewerIdOrderByCreatedAtDesc(userId, pageable)
            .map(this::toResponse);

        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /** Count reviews submitted by current user */
    @GetMapping("/me/count")
    @Operation(summary = "Get total reviews submitted by me")
    public ResponseEntity<ApiResponse<Long>> countMine(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewRepository.countByReviewerId(userId)));
    }

    /** Submit review (authenticated, must have completed appointment) */
    @PostMapping
    @Operation(summary = "Submit a review for a completed appointment")
    public ResponseEntity<ApiResponse<ReviewResponse>> submit(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SubmitReviewRequest request) {

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
            .orElseThrow(() -> AppException.notFound("Appointment not found"));

        // Only the client who had the appointment can review it
        if (!appointment.getClient().getId().equals(userId)) {
            throw AppException.forbidden("You can only review your own appointments");
        }

        if (appointment.getStatus() != AppointmentStatus.COMPLETED && 
            appointment.getStatus() != AppointmentStatus.PENDING_BALANCE && 
            appointment.getStatus() != AppointmentStatus.PAID_FULL) {
            throw AppException.badRequest("You can only review appointments that have taken place");
        }

        if (reviewRepository.existsByAppointmentId(appointment.getId())) {
            throw AppException.conflict("You have already reviewed this appointment");
        }

        User reviewer = userRepository.findById(userId)
            .orElseThrow(() -> AppException.notFound("User not found"));

        Review review = Review.builder()
            .appointment(appointment)
            .reviewer(reviewer)
            .professional(appointment.getProfessional())
            .rating(request.getRating())
            .comment(request.getComment())
            .visible(true)
            .flagged(false)
            .helpfulCount(0)
            .createdAt(OffsetDateTime.now())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Review submitted. Thank you!", toResponse(reviewRepository.save(review))));
    }

    /** Mark a review as helpful */
    @PostMapping("/{id}/helpful")
    @Operation(summary = "Mark a review as helpful")
    public ResponseEntity<ApiResponse<Void>> markHelpful(@PathVariable UUID id) {
        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> AppException.notFound("Review not found"));

        review.setHelpfulCount(review.getHelpfulCount() + 1);
        reviewRepository.save(review);
        return ResponseEntity.ok(ApiResponse.ok("Marked as helpful", null));
    }

    /** Flag a review as inappropriate */
    @PostMapping("/{id}/flag")
    @Operation(summary = "Flag a review as inappropriate")
    public ResponseEntity<ApiResponse<Void>> flag(
            @PathVariable UUID id,
            @RequestBody(required = false) FlagRequest request) {

        Review review = reviewRepository.findById(id)
            .orElseThrow(() -> AppException.notFound("Review not found"));

        review.setFlagged(true);
        review.setFlagReason(request != null ? request.getReason() : null);
        reviewRepository.save(review);
        return ResponseEntity.ok(ApiResponse.ok("Review flagged for moderation", null));
    }

    // ─── Mapper ─────────────────────────────────────────────────────────────

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(
            r.getId(),
            r.getReviewer().getFullName(),
            r.getReviewer().getAvatarUrl(),
            r.getRating(),
            r.getComment(),
            r.getHelpfulCount(),
            r.getCreatedAt()
        );
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    public static class SubmitReviewRequest {
        @NotNull
        private UUID appointmentId;

        @NotNull @Min(1) @Max(5)
        private Short rating;

        @Size(max = 1000)
        private String comment;

        public UUID getAppointmentId() { return appointmentId; }
        public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
        public Short getRating() { return rating; }
        public void setRating(Short rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class FlagRequest {
        @Size(max = 255)
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public record ReviewResponse(
        UUID id,
        String reviewerName,
        String reviewerAvatar,
        Short rating,
        String comment,
        Integer helpfulCount,
        OffsetDateTime createdAt
    ) {}
}
