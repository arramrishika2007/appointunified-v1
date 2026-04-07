package com.appointunified.controller;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.dto.response.AuthResponse;
import com.appointunified.dto.response.BehaviorResponse;
import com.appointunified.entity.User;
import com.appointunified.exception.AppException;
import com.appointunified.repository.UserRepository;
import com.appointunified.service.BehaviorScoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BehaviorScoringService behaviorScoringService;

    @GetMapping("/me")
    @Operation(summary = "Get own profile")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getMyProfile(
            @AuthenticationPrincipal UUID userId) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> AppException.notFound("User not found"));

        return ResponseEntity.ok(ApiResponse.ok(toUserInfo(user)));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update own profile")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> updateProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> AppException.notFound("User not found"));

        if (request.getFullName()  != null) user.setFullName(request.getFullName());
        if (request.getEmail()     != null) {
            if (userRepository.existsByEmail(request.getEmail()) &&
                    !request.getEmail().equals(user.getEmail())) {
                throw AppException.conflict("Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        return ResponseEntity.ok(ApiResponse.ok(toUserInfo(userRepository.save(user))));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Change own password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ChangePasswordRequest request) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> AppException.notFound("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw AppException.badRequest("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully", null));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Deactivate own account")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @AuthenticationPrincipal UUID userId) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> AppException.notFound("User not found"));

        user.setActive(false);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok("Account deactivated. Contact support to reactivate.", null));
    }

    @GetMapping("/{id}/risk-score")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','PROFESSIONAL')")
    @Operation(summary = "Get user's behavior risk score")
    public ResponseEntity<ApiResponse<BehaviorResponse.RiskScore>> getRiskScore(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(behaviorScoringService.getRiskScore(id)));
    }

    @GetMapping("/me/risk-summary")
    @Operation(summary = "Get my behavior risk summary")
    public ResponseEntity<ApiResponse<BehaviorResponse.RiskSummary>> getMyRiskSummary(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(behaviorScoringService.getRiskSummary(userId)));
    }

    private AuthResponse.UserInfo toUserInfo(User u) {
        AuthResponse.UserInfo info = new AuthResponse.UserInfo();
        info.setId(u.getId());
        info.setFullName(u.getFullName());
        info.setPhone(u.getPhone());
        info.setEmail(u.getEmail());
        info.setRole(u.getRole().name());
        info.setAvatarUrl(u.getAvatarUrl());
        info.setVerified(u.isVerified());
        info.setSector(u.getSector() != null ? u.getSector().name() : null);
        return info;
    }

    // ─── Inner DTOs ──────────────────────────────────────────────────────────

    public static class UpdateProfileRequest {
        @Size(min = 2, max = 100)
        private String fullName;

        @Email
        private String email;

        private String avatarUrl;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;

        @Size(min = 6, message = "New password must be at least 6 characters")
        private String newPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
