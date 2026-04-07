package com.appointunified.dto.request;

import com.appointunified.enums.AvailabilityMood;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public class ProfessionalRequest {

    public static class Register {
        @NotBlank
        @Size(max = 255)
        private String displayName;

        @NotBlank
        private String sector;        // HEALTHCARE | GOVERNMENT | SERVICES

        private String specialty;
        private String licenseNumber;
        private String bio;
        private String qualification;
        private Short yearsExperience;

        @DecimalMin("0.0")
        private BigDecimal consultationFee;

        private String city;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private BigDecimal serviceAreaRadiusKm;

        private String upiId;

        @Valid
        private List<AvailabilitySlot> weeklySchedule;

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }
        public String getSpecialty() { return specialty; }
        public void setSpecialty(String specialty) { this.specialty = specialty; }
        public String getLicenseNumber() { return licenseNumber; }
        public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getQualification() { return qualification; }
        public void setQualification(String qualification) { this.qualification = qualification; }
        public Short getYearsExperience() { return yearsExperience; }
        public void setYearsExperience(Short yearsExperience) { this.yearsExperience = yearsExperience; }
        public BigDecimal getConsultationFee() { return consultationFee; }
        public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public BigDecimal getLatitude() { return latitude; }
        public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
        public BigDecimal getLongitude() { return longitude; }
        public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
        public BigDecimal getServiceAreaRadiusKm() { return serviceAreaRadiusKm; }
        public void setServiceAreaRadiusKm(BigDecimal serviceAreaRadiusKm) { this.serviceAreaRadiusKm = serviceAreaRadiusKm; }
        public String getUpiId() { return upiId; }
        public void setUpiId(String upiId) { this.upiId = upiId; }
        public List<AvailabilitySlot> getWeeklySchedule() { return weeklySchedule; }
        public void setWeeklySchedule(List<AvailabilitySlot> weeklySchedule) { this.weeklySchedule = weeklySchedule; }
    }

    public static class Update {
        private String displayName;
        private String bio;
        private String qualification;
        private Short yearsExperience;
        private BigDecimal consultationFee;
        private String city;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String avatarUrl;
        private String coverUrl;
        private Boolean acceptingBookings;
        private String upiId;

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getQualification() { return qualification; }
        public void setQualification(String qualification) { this.qualification = qualification; }
        public Short getYearsExperience() { return yearsExperience; }
        public void setYearsExperience(Short yearsExperience) { this.yearsExperience = yearsExperience; }
        public BigDecimal getConsultationFee() { return consultationFee; }
        public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public BigDecimal getLatitude() { return latitude; }
        public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
        public BigDecimal getLongitude() { return longitude; }
        public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
        public Boolean getAcceptingBookings() { return acceptingBookings; }
        public void setAcceptingBookings(Boolean acceptingBookings) { this.acceptingBookings = acceptingBookings; }
        public String getUpiId() { return upiId; }
        public void setUpiId(String upiId) { this.upiId = upiId; }
    }

    // NEW V1 FEATURE 2: Mood Status Update
    public static class MoodUpdate {
        @NotNull
        private AvailabilityMood mood;

        @Size(max = 200)
        private String note;

        public AvailabilityMood getMood() { return mood; }
        public void setMood(AvailabilityMood mood) { this.mood = mood; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    public static class AvailabilitySlot {
        @NotNull
        @Min(0) @Max(6)
        private Short weekday;

        @NotNull
        private LocalTime startTime;

        @NotNull
        private LocalTime endTime;

        @Min(0) @Max(60)
        private Integer bufferMinutes = 5;

        @Min(10) @Max(240)
        private Integer slotDurationMins = 30;

        public Short getWeekday() { return weekday; }
        public void setWeekday(Short weekday) { this.weekday = weekday; }
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
        public Integer getBufferMinutes() { return bufferMinutes; }
        public void setBufferMinutes(Integer bufferMinutes) { this.bufferMinutes = bufferMinutes; }
        public Integer getSlotDurationMins() { return slotDurationMins; }
        public void setSlotDurationMins(Integer slotDurationMins) { this.slotDurationMins = slotDurationMins; }
    }
}
