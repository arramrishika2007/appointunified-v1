package com.appointunified.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfessionalResponse {

    public static class Summary {
        private UUID id;
        private String displayName;
        private String sector;
        private String specialty;
        private String verificationStatus;
        private BigDecimal ratingAvg;
        private Integer totalReviews;
        private String avatarUrl;
        private BigDecimal consultationFee;
        private String city;
        private Boolean acceptingBookings;
        // NEW: Mood
        private String availabilityMood;
        private String moodNote;
        private String badgeTier;
        private BigDecimal reputationScore;
        // Computed
        private String nextAvailableSlot;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }
        public String getSpecialty() { return specialty; }
        public void setSpecialty(String specialty) { this.specialty = specialty; }
        public String getVerificationStatus() { return verificationStatus; }
        public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
        public BigDecimal getRatingAvg() { return ratingAvg; }
        public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }
        public Integer getTotalReviews() { return totalReviews; }
        public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public BigDecimal getConsultationFee() { return consultationFee; }
        public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public Boolean getAcceptingBookings() { return acceptingBookings; }
        public void setAcceptingBookings(Boolean acceptingBookings) { this.acceptingBookings = acceptingBookings; }
        public String getAvailabilityMood() { return availabilityMood; }
        public void setAvailabilityMood(String availabilityMood) { this.availabilityMood = availabilityMood; }
        public String getMoodNote() { return moodNote; }
        public void setMoodNote(String moodNote) { this.moodNote = moodNote; }
        public String getBadgeTier() { return badgeTier; }
        public void setBadgeTier(String badgeTier) { this.badgeTier = badgeTier; }
        public BigDecimal getReputationScore() { return reputationScore; }
        public void setReputationScore(BigDecimal reputationScore) { this.reputationScore = reputationScore; }
        public String getNextAvailableSlot() { return nextAvailableSlot; }
        public void setNextAvailableSlot(String nextAvailableSlot) { this.nextAvailableSlot = nextAvailableSlot; }
    }

    public static class Detail extends Summary {
        private String bio;
        private String qualification;
        private Short yearsExperience;
        private String coverUrl;
        private String licenseNumber;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private Integer totalCompleted;
        private List<ServiceResponse.Summary> services;
        private List<AvailabilityDay> weeklySchedule;
        private OffsetDateTime joinedAt;
        private OffsetDateTime verificationExpiresAt;

        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getQualification() { return qualification; }
        public void setQualification(String qualification) { this.qualification = qualification; }
        public Short getYearsExperience() { return yearsExperience; }
        public void setYearsExperience(Short yearsExperience) { this.yearsExperience = yearsExperience; }
        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
        public String getLicenseNumber() { return licenseNumber; }
        public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public BigDecimal getLatitude() { return latitude; }
        public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
        public BigDecimal getLongitude() { return longitude; }
        public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
        public Integer getTotalCompleted() { return totalCompleted; }
        public void setTotalCompleted(Integer totalCompleted) { this.totalCompleted = totalCompleted; }
        public List<ServiceResponse.Summary> getServices() { return services; }
        public void setServices(List<ServiceResponse.Summary> services) { this.services = services; }
        public List<AvailabilityDay> getWeeklySchedule() { return weeklySchedule; }
        public void setWeeklySchedule(List<AvailabilityDay> weeklySchedule) { this.weeklySchedule = weeklySchedule; }
        public OffsetDateTime getJoinedAt() { return joinedAt; }
        public void setJoinedAt(OffsetDateTime joinedAt) { this.joinedAt = joinedAt; }
        public OffsetDateTime getVerificationExpiresAt() { return verificationExpiresAt; }
        public void setVerificationExpiresAt(OffsetDateTime verificationExpiresAt) { this.verificationExpiresAt = verificationExpiresAt; }
    }

    public static class AvailabilityDay {
        private Short weekday;
        private String dayName;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer bufferMinutes;
        private Integer slotDurationMins;

        public Short getWeekday() { return weekday; }
        public void setWeekday(Short weekday) { this.weekday = weekday; }
        public String getDayName() { return dayName; }
        public void setDayName(String dayName) { this.dayName = dayName; }
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
