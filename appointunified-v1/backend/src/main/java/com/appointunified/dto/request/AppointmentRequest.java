package com.appointunified.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class AppointmentRequest {

    public static class Create {
        @NotNull
        private UUID professionalId;

        @NotNull
        private UUID serviceId;

        @NotNull
        @Future(message = "Appointment must be in the future")
        private OffsetDateTime startTime;

        @Size(max = 1000)
        private String notes;

        private String priority; // defaults NORMAL

        private boolean virtual = false;

        public UUID getProfessionalId() { return professionalId; }
        public void setProfessionalId(UUID professionalId) { this.professionalId = professionalId; }
        public UUID getServiceId() { return serviceId; }
        public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
        public OffsetDateTime getStartTime() { return startTime; }
        public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public boolean isVirtual() { return virtual; }
        public void setVirtual(boolean virtual) { this.virtual = virtual; }
    }

    public static class Reschedule {
        @NotNull
        @Future
        private OffsetDateTime newStartTime;

        @Size(max = 500)
        private String reason;

        public OffsetDateTime getNewStartTime() { return newStartTime; }
        public void setNewStartTime(OffsetDateTime newStartTime) { this.newStartTime = newStartTime; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class Cancel {
        @Size(max = 500)
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    // NEW V1 FEATURE 3: Save booking draft
    public static class SaveDraft {
        private UUID professionalId;
        private UUID serviceId;
        private Short stepReached;
        private Map<String, Object> draftData; // flexible JSON

        public UUID getProfessionalId() { return professionalId; }
        public void setProfessionalId(UUID professionalId) { this.professionalId = professionalId; }
        public UUID getServiceId() { return serviceId; }
        public void setServiceId(UUID serviceId) { this.serviceId = serviceId; }
        public Short getStepReached() { return stepReached; }
        public void setStepReached(Short stepReached) { this.stepReached = stepReached; }
        public Map<String, Object> getDraftData() { return draftData; }
        public void setDraftData(Map<String, Object> draftData) { this.draftData = draftData; }
    }
}
