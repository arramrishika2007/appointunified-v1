package com.appointunified.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppointmentResponse {

    public static class Summary {
        private UUID id;
        private String status;
        private String priority;
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        private String notes;
        private boolean virtual;
        private String meetLink;
        private String shareToken;     // NEW V1 FEATURE 4
        private String icalUrl;        // NEW V1 FEATURE 4
        private OffsetDateTime createdAt;

        // Nested
        private ProfessionalInfo professional;
        private ServiceInfo service;
        private ClientInfo client;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public OffsetDateTime getStartTime() { return startTime; }
        public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }
        public OffsetDateTime getEndTime() { return endTime; }
        public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public boolean isVirtual() { return virtual; }
        public void setVirtual(boolean virtual) { this.virtual = virtual; }
        public String getMeetLink() { return meetLink; }
        public void setMeetLink(String meetLink) { this.meetLink = meetLink; }
        public String getShareToken() { return shareToken; }
        public void setShareToken(String shareToken) { this.shareToken = shareToken; }
        public String getIcalUrl() { return icalUrl; }
        public void setIcalUrl(String icalUrl) { this.icalUrl = icalUrl; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public ProfessionalInfo getProfessional() { return professional; }
        public void setProfessional(ProfessionalInfo professional) { this.professional = professional; }
        public ServiceInfo getService() { return service; }
        public void setService(ServiceInfo service) { this.service = service; }
        public ClientInfo getClient() { return client; }
        public void setClient(ClientInfo client) { this.client = client; }
    }

    public static class ProfessionalInfo {
        private UUID id;
        private String displayName;
        private String avatarUrl;
        private String sector;
        private String specialty;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }
        public String getSpecialty() { return specialty; }
        public void setSpecialty(String specialty) { this.specialty = specialty; }
    }

    public static class ServiceInfo {
        private UUID id;
        private String name;
        private Integer durationMinutes;
        private BigDecimal price;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    public static class ClientInfo {
        private UUID id;
        private String fullName;
        private String phone;
        private String avatarUrl;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }

    // NEW V1 FEATURE 4: Share/iCal response
    public static class ShareInfo {
        private String shareUrl;
        private String icalUrl;
        private OffsetDateTime expiresAt;

        public String getShareUrl() { return shareUrl; }
        public void setShareUrl(String shareUrl) { this.shareUrl = shareUrl; }
        public String getIcalUrl() { return icalUrl; }
        public void setIcalUrl(String icalUrl) { this.icalUrl = icalUrl; }
        public OffsetDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    }

    // Available slots response
    public static class AvailableSlot {
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        private boolean available;
        private String label; // "9:00 AM", "09:30 AM"

        public OffsetDateTime getStartTime() { return startTime; }
        public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }
        public OffsetDateTime getEndTime() { return endTime; }
        public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    // NEW V1 FEATURE 3: Draft response
    public static class DraftSummary {
        private UUID id;
        private UUID professionalId;
        private String professionalName;
        private UUID serviceId;
        private String serviceName;
        private Short stepReached;
        private Object draftData;
        private OffsetDateTime expiresAt;
        private OffsetDateTime updatedAt;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getProfessionalId() { return professionalId; }
        public void setProfessionalId(UUID professionalId) { this.professionalId = professionalId; }
        public String getProfessionalName() { return professionalName; }
        public void setProfessionalName(String professionalName) { this.professionalName = professionalName; }
        public UUID getServiceId() { return serviceId; }
        public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public Short getStepReached() { return stepReached; }
        public void setStepReached(Short stepReached) { this.stepReached = stepReached; }
        public Object getDraftData() { return draftData; }
        public void setDraftData(Object draftData) { this.draftData = draftData; }
        public OffsetDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
