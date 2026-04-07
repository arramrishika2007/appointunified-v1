package com.appointunified.service;

import com.appointunified.dto.response.AppointmentResponse;
import com.appointunified.dto.response.ProfessionalResponse;
import com.appointunified.entity.User;
import com.appointunified.enums.VerificationStatus;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.ProfessionalRepository;
import com.appointunified.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * NEW V1 FEATURE 1: Smart Slot Recommendation Engine
 *
 * Analyses a user's booking history to surface the most relevant
 * providers and time slots. Unlike a basic "popular providers" feed,
 * this engine personalises by:
 *
 *  - Preferred time-of-day (morning / afternoon / evening)
 *  - Previously booked sectors
 *  - Favourite professionals (repeat bookings)
 *  - Location proximity (city match)
 *  - Booking lead time (how far in advance they usually book)
 *
 * V1 implements a lightweight rules-based scoring engine.
 * V5+ will replace this with an ML model backed by behavior_scores.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlotRecommendationService {

    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final UserRepository userRepository;
    private final ProfessionalService professionalService;

    /**
     * Returns scored slot recommendations for a user.
     *
     * @param userId the authenticated user
     * @param limit  how many recommendations to return
     */
    @Cacheable(cacheNames = "slotRecommendations", key = "#userId.toString() + ':' + #limit")
    public List<RecommendedSlot> getRecommendations(UUID userId, int limit) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return Collections.emptyList();

        // 1. Analyse booking history
        UserProfile profile = buildUserProfile(userId);

        // 2. Fetch candidate professionals
        var candidates = professionalRepository
            .findByVerificationStatus(VerificationStatus.APPROVED,
                PageRequest.of(0, 50))
            .getContent();

        // 3. Score each candidate
        List<ScoredCandidate> scored = candidates.stream()
            .map(p -> {
                double score = 0.0;
                String reason = "TOP_RATED";

                // Boost: repeat provider
                if (profile.favouriteProfessionalIds.contains(p.getId())) {
                    score += 30.0;
                    reason = "FAVOURITE_PROVIDER";
                }

                // Boost: matching sector from history
                if (profile.preferredSector != null &&
                    profile.preferredSector.equals(p.getSector())) {
                    score += 20.0;
                }

                // Boost: same city
                if (user.getEmail() != null && p.getCity() != null &&
                    profile.preferredCity != null &&
                    p.getCity().equalsIgnoreCase(profile.preferredCity)) {
                    score += 15.0;
                }

                // Base: rating contribution
                if (p.getRatingAvg() != null) {
                    score += p.getRatingAvg().doubleValue() * 5;
                }

                // Boost: currently available mood
                if (p.getAvailabilityMood() != null) {
                    switch (p.getAvailabilityMood()) {
                        case AVAILABLE -> score += 10.0;
                        case RUNNING_LATE -> score -= 5.0;
                        case DO_NOT_DISTURB, BUSY -> score -= 15.0;
                        default -> { }
                    }
                }

                return new ScoredCandidate(p.getId(), p.getDisplayName(),
                    p.getSector().name(), score, reason);
            })
            .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed())
            .limit(limit)
            .collect(Collectors.toList());

        // 4. Build recommended slots in preferred time window
        LocalTime slotTime = preferredSlotTime(profile.preferredTimeOfDay);
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        return scored.stream().map(sc -> new RecommendedSlot(
            sc.professionalId(),
            sc.displayName(),
            sc.sector(),
            tomorrow,
            slotTime,
            sc.reason()
        )).collect(Collectors.toList());
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private UserProfile buildUserProfile(UUID userId) {
        var history = appointmentRepository
            .findByClientIdOrderByStartTimeDesc(userId, PageRequest.of(0, 20))
            .getContent();

        if (history.isEmpty()) {
            return new UserProfile(null, null, null, Collections.emptySet(), 2.0);
        }

        // Most common sector
        var sectorFreq = history.stream()
            .collect(Collectors.groupingBy(
                a -> a.getProfessional().getSector(),
                Collectors.counting()));
        var preferredSector = sectorFreq.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

        // Most common city
        var cityFreq = history.stream()
            .filter(a -> a.getProfessional().getCity() != null)
            .collect(Collectors.groupingBy(
                a -> a.getProfessional().getCity(),
                Collectors.counting()));
        var preferredCity = cityFreq.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

        // Preferred time of day
        var hourFreq = history.stream()
            .collect(Collectors.groupingBy(
                a -> a.getStartTime().getHour(),
                Collectors.counting()));
        int preferredHour = hourFreq.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(10);
        String timeOfDay = preferredHour < 12 ? "MORNING"
            : preferredHour < 17 ? "AFTERNOON" : "EVENING";

        // Favourite professionals (booked 2+ times)
        var profFreq = history.stream()
            .collect(Collectors.groupingBy(
                a -> a.getProfessional().getId(),
                Collectors.counting()));
        Set<UUID> favourites = profFreq.entrySet().stream()
            .filter(e -> e.getValue() >= 2)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        // Average lead days
        double avgLead = history.stream()
            .mapToLong(a -> Duration.between(a.getCreatedAt(), a.getStartTime()).toDays())
            .filter(d -> d >= 0)
            .average()
            .orElse(2.0);

        return new UserProfile(preferredSector, preferredCity, timeOfDay, favourites, avgLead);
    }

    private LocalTime preferredSlotTime(String timeOfDay) {
        if (timeOfDay == null) return LocalTime.of(10, 0);
        return switch (timeOfDay) {
            case "AFTERNOON" -> LocalTime.of(14, 0);
            case "EVENING"   -> LocalTime.of(17, 0);
            default          -> LocalTime.of(10, 0);
        };
    }

    // ─── Inner records ───────────────────────────────────────────────────────

    private record UserProfile(
        com.appointunified.enums.Sector preferredSector,
        String preferredCity,
        String preferredTimeOfDay,
        Set<UUID> favouriteProfessionalIds,
        double avgLeadDays
    ) {}

    private record ScoredCandidate(
        UUID professionalId,
        String displayName,
        String sector,
        double score,
        String reason
    ) {}

    public record RecommendedSlot(
        UUID professionalId,
        String professionalName,
        String sector,
        LocalDate suggestedDate,
        LocalTime suggestedTime,
        String reason
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private UUID professionalId;
            private String professionalName;
            private String sector;
            private LocalDate suggestedDate;
            private LocalTime suggestedTime;
            private String reason;

            public Builder professionalId(UUID professionalId) { this.professionalId = professionalId; return this; }
            public Builder professionalName(String professionalName) { this.professionalName = professionalName; return this; }
            public Builder sector(String sector) { this.sector = sector; return this; }
            public Builder suggestedDate(LocalDate suggestedDate) { this.suggestedDate = suggestedDate; return this; }
            public Builder suggestedTime(LocalTime suggestedTime) { this.suggestedTime = suggestedTime; return this; }
            public Builder reason(String reason) { this.reason = reason; return this; }

            public RecommendedSlot build() {
                return new RecommendedSlot(professionalId, professionalName, sector, suggestedDate, suggestedTime, reason);
            }
        }
    }
}
