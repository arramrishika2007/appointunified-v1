package com.appointunified.repository;

import java.util.List;
import java.util.UUID;

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
    List<AISuggestion> findByProfessionalIdAndIsActiveTrueOrderByGeneratedAtDesc(UUID professionalId);
    
    /**
     * Find the latest active suggestion for a professional
     */
    @Query("SELECT a FROM AISuggestion a WHERE a.professional.id = :professionalId AND a.isActive = true ORDER BY a.generatedAt DESC LIMIT 1")
    AISuggestion findLatestActiveSuggestionForProfessional(@Param("professionalId") UUID professionalId);
    
    /**
     * Find all suggestions generated today for a professional
     */
    @Query("SELECT a FROM AISuggestion a WHERE a.professional.id = :professionalId AND DATE(a.generatedAt) = CURRENT_DATE AND a.isActive = true")
    List<AISuggestion> findTodaysSuggestionsForProfessional(@Param("professionalId") UUID professionalId);
    
    /**
     * Find high-impact suggestions across all professionals
     */
    @Query("SELECT a FROM AISuggestion a WHERE a.expectedImpact = 'HIGH' AND a.isActive = true ORDER BY a.generatedAt DESC")
    List<AISuggestion> findHighImpactSuggestions();
}
