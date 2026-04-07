package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.entity.AvailabilityException;
import com.appointunified.entity.Professional;
import com.appointunified.exception.AppException;
import com.appointunified.repository.AvailabilityExceptionRepository;
import com.appointunified.repository.ProfessionalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * NEW V1 FEATURE 5: Provider Business Hours Exception Calendar
 *
 * Lets professionals mark specific dates as:
 *  - HOLIDAY      → full day off (no slots generated)
 *  - PARTIAL      → reduced hours that day
 *  - EXTRA_HOURS  → extended hours beyond normal schedule
 *
 * Complements the weekly Availability table. The slot engine
 * checks this table before generating slots for any date.
 */
@RestController
@RequestMapping("/availability/exceptions")
@RequiredArgsConstructor
@Tag(name = "Availability Exceptions", description = "Holiday and partial-day blocking for professionals")
public class AvailabilityExceptionController {

    private final AvailabilityExceptionRepository exceptionRepository;
    private final ProfessionalRepository professionalRepository;

    @GetMapping("/professional/{professionalId}")
    @Operation(summary = "List exceptions for a professional (public)")
    public ResponseEntity<ApiResponse<List<ExceptionResponse>>> listForProfessional(
            @PathVariable UUID professionalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end   = to   != null ? to   : LocalDate.now().plusMonths(3);

        return ResponseEntity.ok(ApiResponse.ok(
            exceptionRepository.findByProfessionalIdAndDateRange(professionalId, start, end)
                .stream().map(this::toResponse).collect(Collectors.toList())));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "List own exceptions")
    public ResponseEntity<ApiResponse<List<ExceptionResponse>>> listMyExceptions(
            @AuthenticationPrincipal UUID userId) {
        Professional professional = professionalRepository.findByUserId(userId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));

        return ResponseEntity.ok(ApiResponse.ok(
            exceptionRepository.findByProfessionalIdOrderByExceptionDateAsc(professional.getId())
                .stream().map(this::toResponse).collect(Collectors.toList())));
    }

    @PostMapping
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Add an exception (holiday, partial, extra hours)")
    public ResponseEntity<ApiResponse<ExceptionResponse>> addException(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ExceptionRequest request) {

        Professional professional = professionalRepository.findByUserId(userId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));

        // Prevent duplicate for same date
        if (exceptionRepository.existsByProfessionalIdAndExceptionDate(
                professional.getId(), request.getExceptionDate())) {
            throw AppException.conflict("An exception already exists for " + request.getExceptionDate());
        }

        AvailabilityException ex = new AvailabilityException();
        ex.setProfessional(professional);
        ex.setExceptionDate(request.getExceptionDate());
        ex.setExceptionType(request.getExceptionType());
        ex.setStartTime(request.getStartTime());
        ex.setEndTime(request.getEndTime());
        ex.setReason(request.getReason());
        ex.setCreatedAt(OffsetDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Exception added", toResponse(exceptionRepository.save(ex))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Remove an exception")
    public ResponseEntity<ApiResponse<Void>> deleteException(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        AvailabilityException ex = exceptionRepository.findById(id)
            .orElseThrow(() -> AppException.notFound("Exception not found"));

        if (!ex.getProfessional().getUser().getId().equals(userId)) {
            throw AppException.forbidden("Not your exception");
        }

        exceptionRepository.delete(ex);
        return ResponseEntity.ok(ApiResponse.ok("Exception removed", null));
    }

    // ─── Mapper ─────────────────────────────────────────────────────────────

    private ExceptionResponse toResponse(AvailabilityException ex) {
        return new ExceptionResponse(
            ex.getId(),
            ex.getExceptionDate(),
            ex.getExceptionType(),
            ex.getStartTime(),
            ex.getEndTime(),
            ex.getReason()
        );
    }

    // ─── Inner DTOs ──────────────────────────────────────────────────────────

    public static class ExceptionRequest {
        @NotNull
        private LocalDate exceptionDate;

        @NotBlank
        private String exceptionType; // HOLIDAY | PARTIAL | EXTRA_HOURS

        private LocalTime startTime;  // null = full day
        private LocalTime endTime;

        private String reason;

        public LocalDate getExceptionDate() { return exceptionDate; }
        public void setExceptionDate(LocalDate exceptionDate) { this.exceptionDate = exceptionDate; }
        public String getExceptionType() { return exceptionType; }
        public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public record ExceptionResponse(
        UUID id,
        LocalDate exceptionDate,
        String exceptionType,
        LocalTime startTime,
        LocalTime endTime,
        String reason
    ) {}
}
