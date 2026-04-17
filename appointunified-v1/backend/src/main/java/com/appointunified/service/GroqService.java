package com.appointunified.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroqService {

    private static final String FALLBACK_MODEL = "llama-3.3-70b-versatile";
    
    @Value("${groq.api.key:}")
    private String groqApiKey;
    
    @Value("${groq.api.model:" + FALLBACK_MODEL + "}")
    private String groqModel;
    
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final int DEFAULT_MAX_TOKENS = 1024;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * Call Groq API with a simple prompt
     */
    public String generateCompletion(String prompt) {
        return generateCompletion(prompt, DEFAULT_MAX_TOKENS, 0.7f);
    }
    
    /**
     * Call Groq API with custom parameters
     */
    public String generateCompletion(String prompt, int maxTokens, float temperature) {
        if (!isConfigured()) {
            log.warn("Groq API key not configured, returning empty response");
            return "";
        }

        String response = requestCompletion(prompt, maxTokens, temperature, groqModel);
        if (!response.isBlank()) {
            return response;
        }

        if (!FALLBACK_MODEL.equals(groqModel)) {
            log.warn("Groq primary model '{}' returned empty content, retrying with fallback model '{}'", groqModel, FALLBACK_MODEL);
            return requestCompletion(prompt, maxTokens, temperature, FALLBACK_MODEL);
        }

        return "";
    }
    
    /**
     * Generate AI suggestions for a professional's schedule
     */
    public String generateScheduleSuggestions(String professionalData) {
        String prompt = String.format("""
            You are an intelligent schedule optimization assistant for AppointUnified.
            Based on the professional's data below, provide 3 specific, actionable schedule optimizations.
            Return ONLY valid JSON (no markdown, no code blocks) with this exact structure:
            {
              "suggestions": [
                {"title": "...", "description": "...", "expected_impact": "HIGH|MEDIUM|LOW"},
                ...
              ]
            }
            
            Professional Data:
            %s
            """, professionalData);
        
        return generateCompletion(prompt, 1500, 0.5f);
    }
    
    /**
     * Generate system chat response with RAG context
     */
    public String generateChatResponse(String userQuestion, String ragContext) {
        String prompt = String.format("""
            You are AppointUnified's helpful assistant. Only answer using the provided context.
            Never reveal personal data, medical records, government IDs, or financial details.
            If the question is outside the provided context scope, respond: "I can only help with AppointUnified platform questions. For personal queries, please contact support."
            
            Context:
            %s
            
            User Question: %s
            
            Provide a helpful, concise response (max 2 paragraphs).
            """, ragContext, userQuestion);
        
        return generateCompletion(prompt, 800, 0.6f);
    }
    
    /**
     * Check if Groq API is properly configured
     */
    public boolean isConfigured() {
        return groqApiKey != null && !groqApiKey.isBlank();
    }
    
    // ==================== Private Helper Methods ====================
    
    private Map<String, Object> buildRequestBody(String prompt, int maxTokens, float temperature, String model) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        
        body.put("messages", messages);
        
        return body;
    }

    private String requestCompletion(String prompt, int maxTokens, float temperature, String model) {
        try {
            Map<String, Object> requestBody = buildRequestBody(prompt, maxTokens, temperature, model);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractContentFromResponse(response.getBody());
            }

            log.error("Groq API error: model={}, status={}, body={}", model, response.getStatusCode(), response.getBody());
            return "";
        } catch (RestClientResponseException e) {
            log.error("Groq API HTTP error: model={}, status={}, body={}", model, e.getRawStatusCode(), e.getResponseBodyAsString());
            return "";
        } catch (Exception e) {
            log.error("Error calling Groq API with model={}", model, e);
            return "";
        }
    }
    
    private String extractContentFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode firstChoice = choices.get(0);
                JsonNode message = firstChoice.get("message");
                JsonNode content = message != null ? message.get("content") : null;

                if (content == null || content.isNull()) {
                    return "";
                }

                if (content.isTextual()) {
                    return content.asText("").trim();
                }

                // Some providers may return content as an array of parts.
                if (content.isArray()) {
                    StringBuilder merged = new StringBuilder();
                    for (JsonNode part : content) {
                        if (part.has("text")) {
                            merged.append(part.get("text").asText(""));
                        }
                    }
                    return merged.toString().trim();
                }
            }
            
            log.warn("Unexpected Groq API response structure: {}", responseBody);
            return "";
        } catch (Exception e) {
            log.error("Error parsing Groq API response", e);
            return "";
        }
    }
}
