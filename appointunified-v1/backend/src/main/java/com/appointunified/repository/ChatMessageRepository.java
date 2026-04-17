package com.appointunified.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.appointunified.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    
    /**
     * Find all messages for a specific appointment, ordered by sent time descending
     */
    List<ChatMessage> findByAppointmentIdOrderBySentAtDesc(UUID appointmentId, Pageable pageable);

    /**
     * Find the most recent message for an appointment.
     */
    Optional<ChatMessage> findTopByAppointmentIdOrderBySentAtDesc(UUID appointmentId);
    
    /**
     * Find unread messages for a user (who is the receiver)
     */
    List<ChatMessage> findByReceiverIdAndIsReadFalseOrderBySentAtDesc(UUID userId);
    
    /**
     * Count unread messages for a user
     */
    long countByReceiverIdAndIsReadFalse(UUID userId);

    /**
     * Count unread messages in a specific appointment for the receiver.
     */
    long countByAppointmentIdAndReceiverIdAndIsReadFalse(UUID appointmentId, UUID receiverId);
    
    /**
     * Find messages between sender and receiver for an appointment
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.appointment.id = :appointmentId ORDER BY m.sentAt DESC")
    List<ChatMessage> findByAppointmentIdOrderedByTime(@Param("appointmentId") UUID appointmentId, Pageable pageable);
    
    /**
     * Find if appointment has any messages
     */
    @Query("SELECT COUNT(m) > 0 FROM ChatMessage m WHERE m.appointment.id = :appointmentId")
    boolean hasMessagesForAppointment(@Param("appointmentId") UUID appointmentId);
    
    /**
     * Find all unread messages for a specific appointment and receiver
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.appointment.id = :appointmentId AND m.receiver.id = :receiverId AND m.isRead = false")
    List<ChatMessage> findUnreadMessagesForAppointment(
        @Param("appointmentId") UUID appointmentId, 
        @Param("receiverId") UUID receiverId);
}
