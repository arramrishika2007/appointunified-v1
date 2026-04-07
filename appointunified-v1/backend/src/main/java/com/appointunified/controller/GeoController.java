package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.dto.response.GeoResponse;
import com.appointunified.service.GeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/geo")
@RequiredArgsConstructor
@Tag(name = "Geo", description = "Geo-aware travel and route optimization")
public class GeoController {

    private final GeoService geoService;

    @GetMapping("/travel-time")
    @Operation(summary = "Get travel distance and duration between two points")
    public ResponseEntity<ApiResponse<GeoResponse.TravelTime>> getTravelTime(
            @RequestParam double fromLat,
            @RequestParam double fromLng,
            @RequestParam double toLat,
            @RequestParam double toLng) {
        return ResponseEntity.ok(ApiResponse.ok(geoService.getTravelTime(fromLat, fromLng, toLat, toLng)));
    }

    @GetMapping("/route-optimize")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Optimize route for offline appointments for a given day")
    public ResponseEntity<ApiResponse<GeoResponse.RouteOptimize>> routeOptimize(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(geoService.optimizeProfessionalRoute(userId, targetDate)));
    }
}
