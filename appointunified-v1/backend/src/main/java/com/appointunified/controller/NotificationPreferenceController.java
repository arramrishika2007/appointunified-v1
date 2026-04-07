package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.entity.NotificationPreference;
import com.appointunified.entity.User;
import com.appointunified.exception.AppException;
import com.appointunified.repository.NotificationPreferenceRepository;
import com.appointunified.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Per-user channel and reminder settings")
public class NotificationPreferenceController {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get own notification preferences")
    public ResponseEntity<ApiResponse<PrefResponse>> get(
            @AuthenticationPrincipal UUID userId) {

        NotificationPreference pref = getOrCreate(userId);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(pref)));
    }

    @PatchMapping
    @Operation(summary = "Update own notification preferences")
    public ResponseEntity<ApiResponse<PrefResponse>> update(
            @AuthenticationPrincipal UUID userId,
            @RequestBody UpdatePrefRequest request) {

        NotificationPreference pref = getOrCreate(userId);

        if (request.getEmailEnabled()    != null) pref.setEmailEnabled(request.getEmailEnabled());
        if (request.getSmsEnabled()      != null) pref.setSmsEnabled(request.getSmsEnabled());
        if (request.getWhatsappEnabled() != null) pref.setWhatsappEnabled(request.getWhatsappEnabled());
        if (request.getPushEnabled()     != null) pref.setPushEnabled(request.getPushEnabled());
        if (request.getReminderHours()   != null) pref.setReminderHours(request.getReminderHours());
        pref.setUpdatedAt(OffsetDateTime.now());

        return ResponseEntity.ok(ApiResponse.ok(toResponse(preferenceRepository.save(pref))));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private NotificationPreference getOrCreate(UUID userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
            NotificationPreference pref = NotificationPreference.builder()
                .user(user)
                .emailEnabled(true)
                .smsEnabled(true)
                .whatsappEnabled(false)
                .pushEnabled(true)
                .reminderHours((short) 24)
                .updatedAt(OffsetDateTime.now())
                .build();
            return preferenceRepository.save(pref);
        });
    }

    private PrefResponse toResponse(NotificationPreference p) {
        return new PrefResponse(
            p.isEmailEnabled(), p.isSmsEnabled(),
            p.isWhatsappEnabled(), p.isPushEnabled(),
            p.getReminderHours()
        );
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    public static class UpdatePrefRequest {
        private Boolean emailEnabled;
        private Boolean smsEnabled;
        private Boolean whatsappEnabled;
        private Boolean pushEnabled;
        private Short reminderHours;

        public Boolean getEmailEnabled() { return emailEnabled; }
        public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }
        public Boolean getSmsEnabled() { return smsEnabled; }
        public void setSmsEnabled(Boolean smsEnabled) { this.smsEnabled = smsEnabled; }
        public Boolean getWhatsappEnabled() { return whatsappEnabled; }
        public void setWhatsappEnabled(Boolean whatsappEnabled) { this.whatsappEnabled = whatsappEnabled; }
        public Boolean getPushEnabled() { return pushEnabled; }
        public void setPushEnabled(Boolean pushEnabled) { this.pushEnabled = pushEnabled; }
        public Short getReminderHours() { return reminderHours; }
        public void setReminderHours(Short reminderHours) { this.reminderHours = reminderHours; }
    }

    public record PrefResponse(
        boolean emailEnabled,
        boolean smsEnabled,
        boolean whatsappEnabled,
        boolean pushEnabled,
        Short reminderHours
    ) {}
}
