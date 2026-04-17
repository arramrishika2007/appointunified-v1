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

        // NEW V1 FEATURE 5: Payments and Security
        private String meetingToken;
        private Double clientLat;
        private Double clientLon;
        private Integer distanceMeters;
        private String depositStatus;
        private String finalPaymentStatus;
        private BigDecimal totalAmount;

        // Nested
        private ProfessionalInfo professional;
        private ServiceInfo service;
        private ClientInfo client;
        
        // Payment details for online bookings (optional)
        private PaymentInfo pendingPayment;

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
        public String getMeetingToken() { return meetingToken; }
        public void setMeetingToken(String meetingToken) { this.meetingToken = meetingToken; }
        public Double getClientLat() { return clientLat; }
        public void setClientLat(Double clientLat) { this.clientLat = clientLat; }
        public Double getClientLon() { return clientLon; }
        public void setClientLon(Double clientLon) { this.clientLon = clientLon; }
        public Integer getDistanceMeters() { return distanceMeters; }
        public void setDistanceMeters(Integer distanceMeters) { this.distanceMeters = distanceMeters; }
        public String getDepositStatus() { return depositStatus; }
        public void setDepositStatus(String depositStatus) { this.depositStatus = depositStatus; }
        public String getFinalPaymentStatus() { return finalPaymentStatus; }
        public void setFinalPaymentStatus(String finalPaymentStatus) { this.finalPaymentStatus = finalPaymentStatus; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public PaymentInfo getPendingPayment() { return pendingPayment; }
        public void setPendingPayment(PaymentInfo pendingPayment) { this.pendingPayment = pendingPayment; }
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

    public static class BookingForm {
        private UUID appointmentId;
        private String bookingToken;
        private Double distanceKm;
        private OffsetDateTime generatedAt;
        private String content;

        public UUID getAppointmentId() { return appointmentId; }
        public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
        public String getBookingToken() { return bookingToken; }
        public void setBookingToken(String bookingToken) { this.bookingToken = bookingToken; }
        public Double getDistanceKm() { return distanceKm; }
        public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
        public OffsetDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(OffsetDateTime generatedAt) { this.generatedAt = generatedAt; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
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

    public static class MeetingJoinInfo {
        private UUID appointmentId;
        private String meetingToken;
        private boolean canJoin;
        private String joinUrl;
        private String status;
        private String reason;
        private String professionalName;
        private String serviceName;
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;

        public UUID getAppointmentId() { return appointmentId; }
        public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
        public String getMeetingToken() { return meetingToken; }
        public void setMeetingToken(String meetingToken) { this.meetingToken = meetingToken; }
        public boolean isCanJoin() { return canJoin; }
        public void setCanJoin(boolean canJoin) { this.canJoin = canJoin; }
        public String getJoinUrl() { return joinUrl; }
        public void setJoinUrl(String joinUrl) { this.joinUrl = joinUrl; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getProfessionalName() { return professionalName; }
        public void setProfessionalName(String professionalName) { this.professionalName = professionalName; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public OffsetDateTime getStartTime() { return startTime; }
        public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }
        public OffsetDateTime getEndTime() { return endTime; }
        public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }
    }

    public static class PaymentInfo {
        private UUID paymentOrderId;
        private String keyId;
        private String gatewayOrderId;
        private BigDecimal depositAmount;
        private BigDecimal totalAmount;
        private String currency;
        private String status;

        public PaymentInfo() {}

        public PaymentInfo(UUID paymentOrderId, String keyId, String gatewayOrderId, 
                          BigDecimal depositAmount, BigDecimal totalAmount, String currency, String status) {
            this.paymentOrderId = paymentOrderId;
            this.keyId = keyId;
            this.gatewayOrderId = gatewayOrderId;
            this.depositAmount = depositAmount;
            this.totalAmount = totalAmount;
            this.currency = currency;
            this.status = status;
        }

        public UUID getPaymentOrderId() { return paymentOrderId; }
        public void setPaymentOrderId(UUID paymentOrderId) { this.paymentOrderId = paymentOrderId; }
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public String getGatewayOrderId() { return gatewayOrderId; }
        public void setGatewayOrderId(String gatewayOrderId) { this.gatewayOrderId = gatewayOrderId; }
        public BigDecimal getDepositAmount() { return depositAmount; }
        public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
