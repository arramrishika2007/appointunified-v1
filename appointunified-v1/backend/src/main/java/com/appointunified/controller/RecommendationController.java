package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.service.SlotRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Smart slot and provider recommendations")
public class RecommendationController {

    private final SlotRecommendationService recommendationService;

    /**
     * NEW V1 FEATURE 1: Smart personalised slot recommendations
     * GET /api/recommendations/slots?limit=6
     */
    @GetMapping("/slots")
    @Operation(summary = "Get personalised slot recommendations for the authenticated user")
    public ResponseEntity<ApiResponse<List<SlotRecommendationService.RecommendedSlot>>> getSlotRecommendations(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(
                recommendationService.getRecommendations(userId, Math.min(limit, 20))));
    }
}
