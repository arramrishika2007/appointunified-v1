package com.appointunified.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AISuggestionResponse {
    private UUID id;
    private String suggestionTitle;
    private String suggestionDescription;
    private String expectedImpact;  // 'HIGH','MEDIUM','LOW'
    private String suggestionData;  // JSON string
    private OffsetDateTime generatedAt;
    private String generatedByModel;
}
