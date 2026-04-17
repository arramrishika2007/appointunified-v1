package com.appointunified.repository;

import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.appointunified.entity.AISuggestion;

@Repository
public interface AISuggestionRepository extends JpaRepository<AISuggestion, UUID> {
    
    /**
     * Find all active suggestions for a professional, ordered by most recent first
     */
    List<AISuggestion> findByProfessional_IdAndIsActiveTrueOrderByGeneratedAtDesc(UUID professionalId);
    
    /**
     * Find the latest active suggestion for a professional
     */
    AISuggestion findFirstByProfessional_IdAndIsActiveTrueOrderByGeneratedAtDesc(UUID professionalId);
    
    /**
     * Find all suggestions generated today for a professional
     */
    @Query("SELECT a FROM AISuggestion a WHERE a.professional.id = :professionalId AND a.generatedAt >= :startOfDay AND a.generatedAt < :startOfNextDay AND a.isActive = true ORDER BY a.generatedAt DESC")
    List<AISuggestion> findTodaysSuggestionsForProfessional(@Param("professionalId") UUID professionalId,
                                                            @Param("startOfDay") OffsetDateTime startOfDay,
                                                            @Param("startOfNextDay") OffsetDateTime startOfNextDay);
    
    /**
     * Find high-impact suggestions across all professionals
     */
    @Query("SELECT a FROM AISuggestion a WHERE a.expectedImpact = 'HIGH' AND a.isActive = true ORDER BY a.generatedAt DESC")
    List<AISuggestion> findHighImpactSuggestions();
}
