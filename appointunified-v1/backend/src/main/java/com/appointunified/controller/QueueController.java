package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.service.QueueEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/queue")
@RequiredArgsConstructor
@Tag(name = "Queue", description = "V3: Real-time queue management")
public class QueueController {

    private final QueueEngineService queueEngine;

    // ─── Public ─────────────────────────────────────────────────────────────

    @GetMapping("/{professionalId}/status")
    @Operation(summary = "Get live queue board for a professional (public)")
    public ResponseEntity<ApiResponse<QueueEngineService.QueueStatus>> getStatus(
            @PathVariable UUID professionalId) {
        return ResponseEntity.ok(ApiResponse.ok(queueEngine.getStatus(professionalId)));
    }

    @GetMapping("/my-status")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Get live queue board for the authenticated professional")
    public ResponseEntity<ApiResponse<QueueEngineService.QueueStatus>> getMyStatus(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(queueEngine.getMyStatus(userId)));
    }

    @GetMapping("/my-position/{appointmentId}")
    @Operation(summary = "Get own queue position and ETA")
    public ResponseEntity<ApiResponse<QueueEngineService.QueueTokenView>> getMyPosition(
            @PathVariable UUID appointmentId) {
        return ResponseEntity.ok(ApiResponse.ok(queueEngine.getMyPosition(appointmentId)));
    }

    @PostMapping("/join/{appointmentId}")
    @Operation(summary = "Join the queue for an appointment")
    public ResponseEntity<ApiResponse<QueueEngineService.QueueTokenView>> joinQueue(
            @PathVariable UUID appointmentId) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Joined queue!", queueEngine.joinQueue(appointmentId)));
    }

    // ─── Professional ────────────────────────────────────────────────────────

    @PostMapping("/next")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Call the next client in queue")
    public ResponseEntity<ApiResponse<QueueEngineService.QueueTokenView>> callNext(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok("Next client called!", queueEngine.callNext(userId)));
    }

    @PostMapping("/delay")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Trigger a delay and propagate ETA to all waiting clients")
    public ResponseEntity<ApiResponse<QueueEngineService.DelayResult>> triggerDelay(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody DelayRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
            queueEngine.triggerDelay(userId, request.getDelayMinutes(), request.getReason())));
    }

    @PostMapping("/pause")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Pause the queue (e.g. lunch break)")
    public ResponseEntity<ApiResponse<Void>> pauseQueue(
            @AuthenticationPrincipal UUID userId,
            @RequestBody(required = false) PauseRequest request) {
        queueEngine.pauseQueue(userId, request != null ? request.getReason() : null);
        return ResponseEntity.ok(ApiResponse.ok("Queue paused", null));
    }

    @PostMapping("/resume")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Resume a paused queue")
    public ResponseEntity<ApiResponse<Void>> resumeQueue(@AuthenticationPrincipal UUID userId) {
        queueEngine.resumeQueue(userId);
        return ResponseEntity.ok(ApiResponse.ok("Queue resumed", null));
    }

    @PostMapping("/emergency")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Insert an emergency appointment at the front of the queue")
    public ResponseEntity<ApiResponse<QueueEngineService.QueueTokenView>> insertEmergency(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody EmergencyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Emergency slot inserted",
            queueEngine.insertEmergency(userId, request.getAppointmentId(), request.getJustification())));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    @Operation(summary = "Broadcast a message to everyone waiting in queue (Feature 5)")
    public ResponseEntity<ApiResponse<Void>> broadcast(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody BroadcastRequest request) {
        queueEngine.broadcastMessage(userId, request.getMessage(), request.getMessageType());
        return ResponseEntity.ok(ApiResponse.ok("Message broadcast to queue", null));
    }

    // ─── WebSocket endpoint (STOMP) ──────────────────────────────────────────

    /**
     * Client subscribes to /topic/queue/{professionalId}
     * Backend sends queue updates via SimpMessagingTemplate.convertAndSend()
     * This @MessageMapping handles client-sent SUBSCRIBE acks (not required but good practice)
     */
    @MessageMapping("/queue.subscribe.{professionalId}")
    public void handleSubscribe(@DestinationVariable String professionalId) {
        // Subscription is handled by STOMP — just log for observability
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    @Data public static class DelayRequest {
        @Min(1) @Max(240)
        private int delayMinutes;
        private String reason;
    }

    @Data public static class PauseRequest {
        private String reason;
    }

    @Data public static class EmergencyRequest {
        @NotNull private UUID appointmentId;
        @NotBlank private String justification;
    }

    @Data public static class BroadcastRequest {
        @NotBlank @Size(max = 300)
        private String message;
        private String messageType; // INFO | WARNING | DELAY | UPDATE
    }
}
