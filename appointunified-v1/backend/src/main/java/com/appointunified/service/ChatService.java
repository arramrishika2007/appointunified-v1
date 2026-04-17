package com.appointunified.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointunified.dto.request.ChatMessageRequest;
import com.appointunified.dto.response.ChatMessageResponse;
import com.appointunified.dto.response.ChatThreadResponse;
import com.appointunified.entity.Appointment;
import com.appointunified.entity.ChatMessage;
import com.appointunified.entity.User;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.ChatMessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Regex patterns for sensitive data detection
    private static final Pattern GOVT_ID_PATTERN = Pattern.compile(
        "(?i)(aadhar|aadhaar|pan|passport|ssn|license|voter id)\\s*[:\\-]?\\s*\\d{4,}");
    
    private static final Pattern MEDICAL_RECORD_PATTERN = Pattern.compile(
        "(?i)(mrn|medical record|patient id|health record)\\s*[:\\-]?\\s*\\d{3,}");
    
    private static final Pattern FINANCIAL_PATTERN = Pattern.compile(
        "(?i)(account|card|credit|debit|cvv|ifsc|bank)\\s*[:\\-]?\\s*\\d{3,}");
    
    /**
     * Send a message in appointment chat
     * Filters for sensitive data before storing
     */
    @Transactional
    public ChatMessageResponse sendMessage(UUID appointmentId, User sender, ChatMessageRequest request) {
        try {
            // Verify appointment exists and sender is involved
            Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
            if (appointment == null) {
                log.warn("Appointment not found: {}", appointmentId);
                return null;
            }
            
            // Determine receiver (opposite party in appointment)
            User receiver = determineChatReceiver(appointment, sender);
            if (receiver == null) {
                log.warn("Cannot determine chat receiver for appointment={}", appointmentId);
                return null;
            }
            
            // Check and filter message for sensitive data
            ContentFilterResult filterResult = filterSensitiveContent(request.getMessage());
            
            // Create message entity
            ChatMessage message = new ChatMessage();
            message.setAppointment(appointment);
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setMessage(filterResult.isFiltered ? "[BLOCKED: " + filterResult.reason + "]" : request.getMessage());
            message.setIsRead(false);
            message.setSentAt(OffsetDateTime.now());
            message.setFiltered(filterResult.isFiltered);
            message.setFilterReason(filterResult.reason);
            
            ChatMessage saved = chatMessageRepository.save(message);
            
            // Send notification to receiver
            if (!filterResult.isFiltered) {
                notificationService.createNotification(
                    receiver.getId(),
                    "CHAT",
                    "New message from " + sender.getFullName(),
                    request.getMessage(),
                    "/chat/" + appointmentId
                );
            }
            
            ChatMessageResponse responseDto = mapToChatMessageResponse(saved);
            
            // Broadcast via WebSocket explicitly for Real-Time clients natively connected to JaaS/Stomp!
            messagingTemplate.convertAndSend("/topic/chat/" + appointmentId, responseDto);
            
            log.info("Chat message saved and broadcasted: appointment={}, sender={}", appointmentId, sender.getId());
            return responseDto;
            
        } catch (Exception e) {
            log.error("Error sending chat message for appointment={}", appointmentId, e);
            return null;
        }
    }
    
    /**
     * Retrieve chat history for an appointment
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(UUID appointmentId, int page, int pageSize) {
        List<ChatMessage> messages = new ArrayList<>(
            chatMessageRepository.findByAppointmentIdOrderBySentAtDesc(appointmentId, PageRequest.of(page, pageSize))
        );
        Collections.reverse(messages);
        
        return messages.stream()
            .map(this::mapToChatMessageResponse)
            .collect(Collectors.toList());
    }

    /**
     * Build a WhatsApp-style inbox for the current user.
     */
    @Transactional(readOnly = true)
    public List<ChatThreadResponse> getMyThreads(User currentUser) {
        List<Appointment> appointments = appointmentRepository.findChatAppointmentsForUser(currentUser.getId());

        return appointments.stream()
            .map(appointment -> mapToThreadResponse(appointment, currentUser.getId()))
            .filter(Objects::nonNull)
            .sorted(Comparator
                .comparing(ChatThreadResponse::getLastMessageAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ChatThreadResponse::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Mark messages as read
     */
    @Transactional
    public void markMessagesAsRead(UUID appointmentId, UUID userId) {
        try {
            List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessagesForAppointment(appointmentId, userId);
            
            unreadMessages.forEach(msg -> {
                msg.setIsRead(true);
                chatMessageRepository.save(msg);
            });
            
            log.info("Marked {} messages as read for user={}", unreadMessages.size(), userId);
        } catch (Exception e) {
            log.error("Error marking messages as read", e);
        }
    }
    
    /**
     * Get unread message count for a user
     */
    @Transactional(readOnly = true)
    public long getUnreadMessageCount(UUID userId) {
        return chatMessageRepository.countByReceiverIdAndIsReadFalse(userId);
    }
    
    /**
     * Get all unread messages for a user (for notification badge)
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getUnreadMessages(UUID userId) {
        List<ChatMessage> messages = chatMessageRepository.findByReceiverIdAndIsReadFalseOrderBySentAtDesc(userId);
        
        return messages.stream()
            .map(this::mapToChatMessageResponse)
            .collect(Collectors.toList());
    }
    
    // ==================== Private Helper Methods ====================
    
    private User determineChatReceiver(Appointment appointment, User sender) {
        // If sender is the user (client), receiver is professional
        if (appointment.getClient() != null && appointment.getClient().getId().equals(sender.getId())) {
            return appointment.getProfessional() != null ? appointment.getProfessional().getUser() : null;
        }
        // If sender is professional, receiver is the user (client)
        return appointment.getClient();
    }
    
    private ContentFilterResult filterSensitiveContent(String message) {
        if (message == null || message.isBlank()) {
            return new ContentFilterResult(false, null);
        }
        
        // Check for government IDs
        if (GOVT_ID_PATTERN.matcher(message).find()) {
            return new ContentFilterResult(true, "Contains personal identification document number");
        }
        
        // Check for medical records
        if (MEDICAL_RECORD_PATTERN.matcher(message).find()) {
            return new ContentFilterResult(true, "Contains medical information. Please keep discussion general.");
        }
        
        // Check for financial data
        if (FINANCIAL_PATTERN.matcher(message).find()) {
            return new ContentFilterResult(true, "Contains financial information. Please keep discussion general.");
        }
        
        return new ContentFilterResult(false, null);
    }
    
    private ChatMessageResponse mapToChatMessageResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setAppointmentId(message.getAppointment().getId());
        response.setSenderId(message.getSender().getId());
        response.setSenderName(message.getSender().getFullName());
        response.setReceiverId(message.getReceiver().getId());
        response.setReceiverName(message.getReceiver().getFullName());
        response.setMessage(message.getMessage());
        response.setIsRead(message.getIsRead());
        response.setSentAt(message.getSentAt());
        response.setFiltered(message.getFiltered());
        response.setFilterReason(message.getFilterReason());
        return response;
    }

    private ChatThreadResponse mapToThreadResponse(Appointment appointment, UUID currentUserId) {
        if (appointment == null || appointment.getService() == null) {
            return null;
        }

        User otherParticipant = null;
        String otherParticipantRole = null;
        if (appointment.getClient() != null && currentUserId.equals(appointment.getClient().getId())) {
            if (appointment.getProfessional() == null || appointment.getProfessional().getUser() == null) {
                return null;
            }
            otherParticipant = appointment.getProfessional().getUser();
            otherParticipantRole = "PROFESSIONAL";
        } else {
            otherParticipant = appointment.getClient();
            otherParticipantRole = "CLIENT";
        }

        if (otherParticipant == null) {
            return null;
        }

        ChatMessage latestMessage = chatMessageRepository.findTopByAppointmentIdOrderBySentAtDesc(appointment.getId())
            .orElse(null);
        long unreadCount = chatMessageRepository.countByAppointmentIdAndReceiverIdAndIsReadFalse(
            appointment.getId(), currentUserId);

        ChatThreadResponse response = new ChatThreadResponse();
        response.setAppointmentId(appointment.getId());
        response.setAppointmentStatus(appointment.getStatus() != null ? appointment.getStatus().name() : null);
        response.setAppointmentPriority(appointment.getPriority() != null ? appointment.getPriority().name() : null);
        response.setStartTime(appointment.getStartTime());
        response.setEndTime(appointment.getEndTime());
        response.setVirtual(appointment.isVirtual());
        response.setServiceName(appointment.getService().getName());
        response.setServiceDurationMinutes(appointment.getService().getDurationMinutes());
        response.setOtherParticipantId(otherParticipant.getId());
        response.setOtherParticipantName(otherParticipant.getFullName());
        response.setOtherParticipantAvatarUrl(otherParticipant.getAvatarUrl());
        response.setOtherParticipantRole(otherParticipantRole);
        response.setUnreadCount(unreadCount);
        response.setHasMessages(latestMessage != null);
        response.setLastMessageAt(latestMessage != null ? latestMessage.getSentAt() : appointment.getCreatedAt());

        String previewText = latestMessage != null
            ? latestMessage.getMessage()
            : "Booked " + appointment.getService().getName() + " with " + otherParticipant.getFullName();
        response.setPreviewText(previewText);
        return response;
    }
    
    // Inner class for filter result
    private static class ContentFilterResult {
        boolean isFiltered;
        String reason;
        
        ContentFilterResult(boolean isFiltered, String reason) {
              this.isFiltered = isFiltered;
            this.reason = reason;
        }
    }
}
