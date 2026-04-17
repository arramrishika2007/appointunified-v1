package com.appointunified.controller;

import com.appointunified.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping("/{bookingId}/join-token")
    public ResponseEntity<MeetingJoinResponse> getJoinToken(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UUID userId) {
        MeetingJoinResponse response = meetingService.generateJoinToken(bookingId, userId);
        return ResponseEntity.ok(response);
    }
    
    public record MeetingJoinResponse(
        String roomName,
        String jwtToken,
        String status
    ) {}
}
