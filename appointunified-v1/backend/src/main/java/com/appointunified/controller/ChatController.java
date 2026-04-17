package com.appointunified.controller;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.appointunified.dto.request.ChatMessageRequest;
import com.appointunified.dto.response.ChatMessageResponse;
import com.appointunified.dto.response.ChatThreadResponse;
import com.appointunified.entity.User;
import com.appointunified.repository.UserRepository;
import com.appointunified.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/chat")
@Slf4j
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
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
     * Get chat history for an appointment
     * GET /api/chat/{appointmentId}/messages?page=0&pageSize=20
     */
    @GetMapping("/{appointmentId}/messages")
    @PreAuthorize("hasAnyRole('PUBLIC', 'PROFESSIONAL')")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @PathVariable UUID appointmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        
        try {
            User currentUser = resolveCurrentUser(authentication);
            if (currentUser == null) {
                log.warn("User not found: {}", authentication.getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Verify user has access to this appointment's chat
            // (This would normally be checked in a separate method)
            
            List<ChatMessageResponse> messages = chatService.getChatHistory(appointmentId, page, pageSize);
            log.info("Retrieved chat history for appointment={}", appointmentId);
            
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error retrieving chat history for appointment={}", appointmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Send a message in appointment chat
     * POST /api/chat/{appointmentId}/send
     */
    @PostMapping("/{appointmentId}/send")
    @PreAuthorize("hasAnyRole('PUBLIC', 'PROFESSIONAL')")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable UUID appointmentId,
            @RequestBody ChatMessageRequest request,
            Authentication authentication) {
        
        try {
            // Validate request
            if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
                log.warn("Invalid chat message request for appointment={}", appointmentId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            User currentUser = resolveCurrentUser(authentication);
            if (currentUser == null) {
                log.warn("User not found: {}", authentication.getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            ChatMessageResponse response = chatService.sendMessage(appointmentId, currentUser, request);
            
            if (response == null) {
                log.warn("Failed to send message for appointment={}", appointmentId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            
            log.info("Message sent for appointment={} by user={}", appointmentId, currentUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Error sending chat message for appointment={}", appointmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Mark messages as read
     * POST /api/chat/{appointmentId}/read
     */
    @PostMapping("/{appointmentId}/read")
    @PreAuthorize("hasAnyRole('PUBLIC', 'PROFESSIONAL')")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable UUID appointmentId,
            Authentication authentication) {
        
        try {
            User currentUser = resolveCurrentUser(authentication);
            if (currentUser == null) {
                log.warn("User not found: {}", authentication.getName());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            chatService.markMessagesAsRead(appointmentId, currentUser.getId());
            log.info("Messages marked as read for appointment={}, user={}", appointmentId, currentUser.getId());
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Error marking messages as read for appointment={}", appointmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get unread message count for current user
     * GET /api/chat/unread-count
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('PUBLIC', 'PROFESSIONAL')")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication authentication) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            long unreadCount = chatService.getUnreadMessageCount(currentUser.getId());
            return ResponseEntity.ok(new UnreadCountResponse(unreadCount));
            
        } catch (Exception e) {
            log.error("Error retrieving unread message count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get booked chat threads for the current user.
     * GET /api/chat/threads
     */
    @GetMapping("/threads")
    @PreAuthorize("hasAnyRole('PUBLIC', 'PROFESSIONAL')")
    public ResponseEntity<List<ChatThreadResponse>> getMyThreads(Authentication authentication) {
        try {
            User currentUser = resolveCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            return ResponseEntity.ok(chatService.getMyThreads(currentUser));
        } catch (Exception e) {
            log.error("Error retrieving chat threads", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Response DTO for unread count
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.AllArgsConstructor
    public static class UnreadCountResponse {
        private long unreadCount;
    }
}
