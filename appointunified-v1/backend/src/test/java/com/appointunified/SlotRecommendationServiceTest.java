package com.appointunified;

import com.appointunified.service.SlotRecommendationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for V1 Feature 1: Smart Slot Recommendation Engine
 */
class SlotRecommendationServiceTest {

    @Test
    void recommendedSlot_hasCorrectFields() {
        var slot = SlotRecommendationService.RecommendedSlot.builder()
            .professionalId(java.util.UUID.randomUUID())
            .professionalName("Dr. Sharma")
            .sector("HEALTHCARE")
            .suggestedDate(LocalDate.now().plusDays(1))
            .suggestedTime(LocalTime.of(10, 0))
            .reason("FAVOURITE_PROVIDER")
            .build();

        assertThat(slot.professionalName()).isEqualTo("Dr. Sharma");
        assertThat(slot.sector()).isEqualTo("HEALTHCARE");
        assertThat(slot.reason()).isEqualTo("FAVOURITE_PROVIDER");
        assertThat(slot.suggestedDate()).isAfter(LocalDate.now());
    }

    @Test
    void recommendedSlot_suggestedDateIsFuture() {
        var slot = SlotRecommendationService.RecommendedSlot.builder()
            .professionalId(java.util.UUID.randomUUID())
            .professionalName("Test Professional")
            .sector("SERVICES")
            .suggestedDate(LocalDate.now().plusDays(2))
            .suggestedTime(LocalTime.of(14, 0))
            .reason("TOP_RATED")
            .build();

        assertThat(slot.suggestedDate()).isAfterOrEqualTo(LocalDate.now());
    }
}
