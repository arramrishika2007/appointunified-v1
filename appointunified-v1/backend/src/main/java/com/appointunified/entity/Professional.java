package com.appointunified.entity;

import com.appointunified.enums.AvailabilityMood;
import com.appointunified.enums.BadgeTier;
import com.appointunified.enums.Sector;
import com.appointunified.enums.VerificationStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "professionals")
public class Professional {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "sector")
    private Sector sector;

    @Column(length = 255)
    private String specialty;

    @Column(name = "license_number", unique = true, length = 100)
    private String licenseNumber;

    @Column(name = "license_hash")
    private String licenseHash;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "verification_status", nullable = false, columnDefinition = "verification_status")
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "total_completed")
    private Integer totalCompleted = 0;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String qualification;

    @Column(name = "years_experience")
    private Short yearsExperience;

    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "service_area_radius_km", precision = 6, scale = 2)
    private BigDecimal serviceAreaRadiusKm;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 100)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "is_accepting_bookings", nullable = false)
    private boolean acceptingBookings = true;

    // ─── NEW V1 FEATURE 2: Provider Mood / Live Status ─────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "availability_mood", length = 20)
    private AvailabilityMood availabilityMood = AvailabilityMood.AVAILABLE;

    @Column(name = "mood_note", length = 200)
    private String moodNote;

    @Column(name = "mood_updated_at")
    private OffsetDateTime moodUpdatedAt;
    // ────────────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "badge_tier", nullable = false, columnDefinition = "badge_tier")
    private BadgeTier badgeTier = BadgeTier.NONE;

    @Column(name = "badge_awarded_at")
    private OffsetDateTime badgeAwardedAt;

    @Column(name = "badge_reviewed_at")
    private OffsetDateTime badgeReviewedAt;

    @Column(name = "verification_expires_at")
    private OffsetDateTime verificationExpiresAt;

    @Column(name = "reverification_notified", nullable = false)
    private boolean reverificationNotified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Sector getSector() { return sector; }
    public void setSector(Sector sector) { this.sector = sector; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    public String getLicenseHash() { return licenseHash; }
    public void setLicenseHash(String licenseHash) { this.licenseHash = licenseHash; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public BigDecimal getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; }
    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }
    public Integer getTotalCompleted() { return totalCompleted; }
    public void setTotalCompleted(Integer totalCompleted) { this.totalCompleted = totalCompleted; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public Short getYearsExperience() { return yearsExperience; }
    public void setYearsExperience(Short yearsExperience) { this.yearsExperience = yearsExperience; }
    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }
    public BigDecimal getServiceAreaRadiusKm() { return serviceAreaRadiusKm; }
    public void setServiceAreaRadiusKm(BigDecimal serviceAreaRadiusKm) { this.serviceAreaRadiusKm = serviceAreaRadiusKm; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public boolean isAcceptingBookings() { return acceptingBookings; }
    public void setAcceptingBookings(boolean acceptingBookings) { this.acceptingBookings = acceptingBookings; }
    public AvailabilityMood getAvailabilityMood() { return availabilityMood; }
    public void setAvailabilityMood(AvailabilityMood availabilityMood) { this.availabilityMood = availabilityMood; }
    public String getMoodNote() { return moodNote; }
    public void setMoodNote(String moodNote) { this.moodNote = moodNote; }
    public OffsetDateTime getMoodUpdatedAt() { return moodUpdatedAt; }
    public void setMoodUpdatedAt(OffsetDateTime moodUpdatedAt) { this.moodUpdatedAt = moodUpdatedAt; }
    public BadgeTier getBadgeTier() { return badgeTier; }
    public void setBadgeTier(BadgeTier badgeTier) { this.badgeTier = badgeTier; }
    public OffsetDateTime getBadgeAwardedAt() { return badgeAwardedAt; }
    public void setBadgeAwardedAt(OffsetDateTime badgeAwardedAt) { this.badgeAwardedAt = badgeAwardedAt; }
    public OffsetDateTime getBadgeReviewedAt() { return badgeReviewedAt; }
    public void setBadgeReviewedAt(OffsetDateTime badgeReviewedAt) { this.badgeReviewedAt = badgeReviewedAt; }
    public OffsetDateTime getVerificationExpiresAt() { return verificationExpiresAt; }
    public void setVerificationExpiresAt(OffsetDateTime verificationExpiresAt) { this.verificationExpiresAt = verificationExpiresAt; }
    public boolean isReverificationNotified() { return reverificationNotified; }
    public void setReverificationNotified(boolean reverificationNotified) { this.reverificationNotified = reverificationNotified; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
