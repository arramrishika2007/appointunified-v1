package com.appointunified.controller;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.dto.response.NotificationResponse;
import com.appointunified.entity.User;
import com.appointunified.repository.UserRepository;
import com.appointunified.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        String principal = authentication.getName();

        try {
            UUID userId = UUID.fromString(principal);
            Optional<User> byId = userRepository.findById(userId);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (IllegalArgumentException ignored) {
            // Principal is not a UUID, try resolving as email for compatibility.
        }

        return userRepository.findByEmail(principal).orElse(null);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('PUBLIC','PROFESSIONAL','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        User user = resolveCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("User not found"));
        }

        Page<NotificationResponse> notifications = notificationService.getUserNotifications(user.getId(), page, pageSize);
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }

    @GetMapping("/me/unread-count")
    @PreAuthorize("hasAnyRole('PUBLIC','PROFESSIONAL','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(Authentication authentication) {
        User user = resolveCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("User not found"));
        }

        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(new UnreadCountResponse(count)));
    }

    @GetMapping("/me/recent")
    @PreAuthorize("hasAnyRole('PUBLIC','PROFESSIONAL','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getRecentNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "7") int days) {
        User user = resolveCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("User not found"));
        }

        List<NotificationResponse> notifications = notificationService.getRecentNotifications(user.getId(), days);
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('PUBLIC','PROFESSIONAL','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        notificationService.markNotificationAsRead(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAnyRole('PUBLIC','PROFESSIONAL','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        User user = resolveCurrentUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("User not found"));
        }

        notificationService.markAllNotificationsAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record UnreadCountResponse(long unreadCount) {}
}
