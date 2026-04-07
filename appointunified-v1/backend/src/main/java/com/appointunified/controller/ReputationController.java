package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.entity.ReputationScore;
import com.appointunified.service.ReputationScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/reputation")
@RequiredArgsConstructor
@Tag(name = "Reputation V2", description = "Reputation score endpoints")
public class ReputationController {

    private final ReputationScoreService reputationScoreService;

    @GetMapping("/professionals/{professionalId}")
    @Operation(summary = "Get reputation score for a professional")
    public ResponseEntity<ApiResponse<ReputationScore>> getReputation(@PathVariable UUID professionalId) {
        return ResponseEntity.ok(ApiResponse.ok(reputationScoreService.getOrCreate(professionalId)));
    }

    @PostMapping("/professionals/{professionalId}/recompute")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Force recompute reputation for a professional")
    public ResponseEntity<ApiResponse<ReputationScore>> recompute(@PathVariable UUID professionalId) {
        return ResponseEntity.ok(ApiResponse.ok(reputationScoreService.recompute(professionalId)));
    }
}
