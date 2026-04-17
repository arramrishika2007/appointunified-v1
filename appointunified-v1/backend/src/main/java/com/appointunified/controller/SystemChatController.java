package com.appointunified.controller;

import java.util.UUID;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appointunified.dto.response.SystemChatResponse;
import com.appointunified.entity.User;
import com.appointunified.repository.UserRepository;
import com.appointunified.service.SystemChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/system-chat")
@Slf4j
@RequiredArgsConstructor
public class SystemChatController {
    
    private final SystemChatService systemChatService;
    private final UserRepository userRepository;

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        String principal = authentication.getName();

        try {
            UUID userId = UUID.fromString(principal);
            Optional<User> byId = userRepository.findById(userId);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (IllegalArgumentException ignored) {
            // Principal is not a UUID, try resolving as email for compatibility.
        }

        return userRepository.findByEmail(principal).orElse(null);
    }
    
    /**
     * POST /api/system-chat/ask
     * Ask the AI chatbot a question about the platform
     * Supports contextual Q&A based on professional profile or booking
     * Auth: Any authenticated user
     */
    @PostMapping("/ask")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SystemChatResponse> askQuestion(
            @RequestParam String question,
            @RequestParam(defaultValue = "general") String contextType,
            @RequestParam(required = false) String contextId,
            Authentication authentication) {
        
        try {
            if (question == null || question.isBlank()) {
                return ResponseEntity.badRequest().body(
                    new SystemChatResponse(question, "Please ask a question.", "ERROR")
                );
            }
            
            User currentUser = resolveCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(401).body(
                    new SystemChatResponse(question, "Unauthorized user", "ERROR")
                );
            }
            UUID contextUUID = null;
            
            if (contextId != null && !contextId.isBlank()) {
                try {
                    contextUUID = UUID.fromString(contextId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid context ID: {}", contextId);
                }
            }
            
            SystemChatResponse response = systemChatService.answerQuestion(
                currentUser, question, contextType, contextUUID);
            
            log.info("System chat response generated: user={}, contextType={}, question_length={}", 
                currentUser.getId(), contextType, question.length());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in system chat endpoint", e);
            return ResponseEntity.internalServerError().body(
                new SystemChatResponse(question, "An error occurred. Please try again.", "ERROR")
            );
        }
    }
}
