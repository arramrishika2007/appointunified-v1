package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.entity.ClientQueuePreference;
import com.appointunified.entity.User;
import com.appointunified.exception.AppException;
import com.appointunified.repository.ClientQueuePreferenceRepository;
import com.appointunified.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * NEW V3 FEATURE 3: Per-client queue notification preferences
 */
@RestController
@RequestMapping("/queue/preferences")
@RequiredArgsConstructor
@Tag(name = "Queue Preferences", description = "V3: Per-user queue notification settings")
public class QueuePreferenceController {

    private final ClientQueuePreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get own queue preferences")
    public ResponseEntity<ApiResponse<PrefView>> getPreferences(
            @AuthenticationPrincipal UUID userId) {
        ClientQueuePreference pref = getOrCreate(userId);
        return ResponseEntity.ok(ApiResponse.ok(toView(pref)));
    }

    @PatchMapping
    @Operation(summary = "Update queue preferences")
    public ResponseEntity<ApiResponse<PrefView>> updatePreferences(
            @AuthenticationPrincipal UUID userId,
            @RequestBody UpdatePrefRequest request) {

        ClientQueuePreference pref = getOrCreate(userId);
        if (request.getNotifyAtPosition() != null)   pref.setNotifyAtPosition(request.getNotifyAtPosition());
        if (request.getPreferSmsOverPush() != null)  pref.setPreferSmsOverPush(request.getPreferSmsOverPush());
        if (request.getAutoCheckInEnabled() != null) pref.setAutoCheckInEnabled(request.getAutoCheckInEnabled());
        if (request.getShowRealtimeEta() != null)    pref.setShowRealtimeEta(request.getShowRealtimeEta());
        pref.setUpdatedAt(OffsetDateTime.now());

        return ResponseEntity.ok(ApiResponse.ok(toView(preferenceRepository.save(pref))));
    }

    private ClientQueuePreference getOrCreate(UUID userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
            return preferenceRepository.save(
                ClientQueuePreference.builder().user(user).build());
        });
    }

    private PrefView toView(ClientQueuePreference p) {
        return new PrefView(p.getNotifyAtPosition(), p.isPreferSmsOverPush(),
            p.isAutoCheckInEnabled(), p.isShowRealtimeEta());
    }

    @Data public static class UpdatePrefRequest {
        private Integer notifyAtPosition;
        private Boolean preferSmsOverPush;
        private Boolean autoCheckInEnabled;
        private Boolean showRealtimeEta;
    }

    public record PrefView(
        int notifyAtPosition, boolean preferSmsOverPush,
        boolean autoCheckInEnabled, boolean showRealtimeEta
    ) {}
}
