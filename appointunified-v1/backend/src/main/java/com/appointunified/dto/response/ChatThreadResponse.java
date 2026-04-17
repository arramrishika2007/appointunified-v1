package com.appointunified.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatThreadResponse {
    private UUID appointmentId;
    private String appointmentStatus;
    private String appointmentPriority;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private boolean virtual;
    private String serviceName;
    private Integer serviceDurationMinutes;
    private UUID otherParticipantId;
    private String otherParticipantName;
    private String otherParticipantAvatarUrl;
    private String otherParticipantRole;
    private String previewText;
    private OffsetDateTime lastMessageAt;
    private long unreadCount;
    private boolean hasMessages;

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(UUID appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getAppointmentStatus() {
        return appointmentStatus;
    }

    public void setAppointmentStatus(String appointmentStatus) {
        this.appointmentStatus = appointmentStatus;
    }

    public String getAppointmentPriority() {
        return appointmentPriority;
    }

    public void setAppointmentPriority(String appointmentPriority) {
        this.appointmentPriority = appointmentPriority;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Integer getServiceDurationMinutes() {
        return serviceDurationMinutes;
    }

    public void setServiceDurationMinutes(Integer serviceDurationMinutes) {
        this.serviceDurationMinutes = serviceDurationMinutes;
    }

    public UUID getOtherParticipantId() {
        return otherParticipantId;
    }

    public void setOtherParticipantId(UUID otherParticipantId) {
        this.otherParticipantId = otherParticipantId;
    }

    public String getOtherParticipantName() {
        return otherParticipantName;
    }

    public void setOtherParticipantName(String otherParticipantName) {
        this.otherParticipantName = otherParticipantName;
    }

    public String getOtherParticipantAvatarUrl() {
        return otherParticipantAvatarUrl;
    }

    public void setOtherParticipantAvatarUrl(String otherParticipantAvatarUrl) {
        this.otherParticipantAvatarUrl = otherParticipantAvatarUrl;
    }

    public String getOtherParticipantRole() {
        return otherParticipantRole;
    }

    public void setOtherParticipantRole(String otherParticipantRole) {
        this.otherParticipantRole = otherParticipantRole;
    }

    public String getPreviewText() {
        return previewText;
    }

    public void setPreviewText(String previewText) {
        this.previewText = previewText;
    }

    public OffsetDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(OffsetDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isHasMessages() {
        return hasMessages;
    }

    public void setHasMessages(boolean hasMessages) {
        this.hasMessages = hasMessages;
    }
}