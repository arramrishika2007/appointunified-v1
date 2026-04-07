package com.appointunified.dto.response;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public class WaitlistResponse {

    public static class Summary {
        private UUID id;
        private UUID professionalId;
        private String professionalName;
        private UUID serviceId;
        private String serviceName;
        private boolean notified;
        private OffsetDateTime notifiedAt;
        private LocalTime preferredTimeFrom;
        private LocalTime preferredTimeTo;
        private OffsetDateTime createdAt;
        private OffsetDateTime expiresAt;

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
        public boolean isNotified() { return notified; }
        public void setNotified(boolean notified) { this.notified = notified; }
        public OffsetDateTime getNotifiedAt() { return notifiedAt; }
        public void setNotifiedAt(OffsetDateTime notifiedAt) { this.notifiedAt = notifiedAt; }
        public LocalTime getPreferredTimeFrom() { return preferredTimeFrom; }
        public void setPreferredTimeFrom(LocalTime preferredTimeFrom) { this.preferredTimeFrom = preferredTimeFrom; }
        public LocalTime getPreferredTimeTo() { return preferredTimeTo; }
        public void setPreferredTimeTo(LocalTime preferredTimeTo) { this.preferredTimeTo = preferredTimeTo; }
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        public OffsetDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    }
}
