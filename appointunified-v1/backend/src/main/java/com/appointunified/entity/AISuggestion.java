package com.appointunified.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AISuggestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;
    
    @Column(name = "suggestion_title", nullable = false)
    private String suggestionTitle;
    
    @Column(name = "suggestion_description", nullable = false)
    private String suggestionDescription;
    
    @Column(name = "expected_impact")
    private String expectedImpact;  // 'HIGH','MEDIUM','LOW'
    
    @Column(name = "suggestion_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String suggestionData;  // JSON string with supporting data from Groq
    
    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;
    
    @Column(name = "generated_by_model")
    private String generatedByModel = "llama3-70b";
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
