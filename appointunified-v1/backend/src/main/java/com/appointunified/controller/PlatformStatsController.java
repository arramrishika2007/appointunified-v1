package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.dto.response.PlatformStatsResponse;
import com.appointunified.entity.Professional;
import com.appointunified.enums.VerificationStatus;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.ProfessionalRepository;
import com.appointunified.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public/stats")
@RequiredArgsConstructor
@Tag(name = "Public Stats", description = "Public endpoints for landing page metrics")
public class PlatformStatsController {

    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentRepository appointmentRepository;

    @GetMapping
    @Operation(summary = "Get platform-wide statistics for the landing page")
    public ResponseEntity<ApiResponse<PlatformStatsResponse>> getPlatformStats() {
        long totalUsers = userRepository.count();
        long totalProfessionals = professionalRepository.countByVerificationStatus(VerificationStatus.APPROVED);
        long totalAppointments = appointmentRepository.count();

        // Get recent 50 approved professionals for map pins
        List<Professional> recentPros = professionalRepository
                .findByVerificationStatusAndAcceptingBookingsTrue(VerificationStatus.APPROVED, org.springframework.data.domain.PageRequest.of(0, 50))
                .getContent();

        List<PlatformStatsResponse.MapPin> pins = recentPros.stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .map(p -> PlatformStatsResponse.MapPin.builder()
                        .id(p.getId())
                        .displayName(p.getDisplayName())
                        .sector(p.getSector() != null ? p.getSector().name() : null)
                        .specialty(p.getSpecialty())
                        .latitude(p.getLatitude())
                        .longitude(p.getLongitude())
                        .build())
                .collect(Collectors.toList());

        PlatformStatsResponse response = PlatformStatsResponse.builder()
                .totalProfessionals(totalProfessionals)
                .totalUsers(totalUsers)
                .totalAppointments(totalAppointments)
                .mapPins(pins)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Stats retrieved", response));
    }
}
