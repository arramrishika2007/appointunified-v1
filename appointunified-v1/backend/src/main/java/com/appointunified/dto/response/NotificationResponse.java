package com.appointunified.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.appointunified.entity.Notification.NotificationType;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        String actionUrl,
        Boolean isRead,
        Boolean isArchived,
        OffsetDateTime readAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}