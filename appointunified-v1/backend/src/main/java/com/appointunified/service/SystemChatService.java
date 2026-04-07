package com.appointunified.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointunified.dto.response.SystemChatResponse;
import com.appointunified.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemChatService {
    
    private final GroqService groqService;
    private final ProfessionalService professionalService;
    private final AppointmentService appointmentService;
    
    /**
     * Answer a user's question about the platform using RAG + Groq API
     */
    @Transactional
    public SystemChatResponse answerQuestion(User user, String question, String contextType, UUID contextId) {
        try {
            if (!groqService.isConfigured()) {
                return new SystemChatResponse(
                    question,
                    "I'm currently offline. Please try again later or contact support.",
                    "OFFLINE"
                );
            }
            
            // Build RAG context based on context type
            String ragContext = buildRAGContext(user, contextType, contextId);
            
            // Call Groq API with context
            String response = groqService.generateChatResponse(question, ragContext);
            
            if (response.isBlank()) {
                log.warn("Groq API returned empty response for question: {}", question);
                return new SystemChatResponse(question, 
                    "I couldn't generate a response. Please try rephrasing your question.", 
                    "ERROR");
            }
            
            log.info("Generated chat response for user={}, contextType={}", user.getId(), contextType);
            return new SystemChatResponse(question, response, "SUCCESS");
            
        } catch (Exception e) {
            log.error("Error answering system chat question", e);
            return new SystemChatResponse(question, 
                "An error occurred processing your question. Please try again.", 
                "ERROR");
        }
    }
    
    // ==================== Private Helper Methods ====================
    
    private String buildRAGContext(User user, String contextType, UUID contextId) {
        StringBuilder context = new StringBuilder();
        
        // Common platform knowledge
        context.append(getPlatformKnowledge());
        
        // Context-specific information
        if ("professional".equals(contextType) && contextId != null) {
            context.append("\n\n").append(getProfessionalContext(contextId));
        } else if ("booking".equals(contextType) && contextId != null) {
            context.append("\n\n").append(getBookingContext(contextId));
        } else if ("general".equals(contextType)) {
            context.append("\n\n").append(getUserBookingContext(user.getId()));
        }
        
        return context.toString();
    }
    
    private String getPlatformKnowledge() {
        return """
            AppointUnified Platform Knowledge:
            
            WHAT IS APPOINTUNIFIED?
            - A dynamic scheduling platform for Healthcare, Government, and Services sectors
            - Enables easy booking of appointments with verified professionals
            - Supports both online (virtual) and offline (in-person) appointments
            - Real-time queue management and notifications
            
            HOW TO BOOK AN APPOINTMENT:
            1. Select a sector (Healthcare, Government, Services)
            2. Search for a professional by name, specialty, or location
            3. Choose a service and available time slot
            4. Select online (virtual) or offline (in-person) mode
            5. Enter appointment details and confirm
            6. Make payment (split into two parts: before and after appointment)
            
            APPOINTMENT MODES:
            - Online: Virtual meeting via Jitsi. You receive a meeting token to join at appointment time.
            - Offline: In-person at professional's location or your location. Professional can see your address and travel time.
            
            PAYMENT PROCESS:
            - Half payment due before appointment (generated UPI QR code)
            - Half payment due after appointment completion
            - Payments via UPI (India's Unified Payments Interface)
            
            CANCELLATION & RESCHEDULING:
            - Cancel up to 24 hours before appointment time
            - Cancellation fees may apply (set by professional)
            - Reschedule to a different time slot if professional has availability
            - If cancelled, slot opens up for other users on waitlist
            
            RATINGS & REVIEWS:
            - Leave a review after appointment completion
            - Rate professionals 1-5 stars
            - Reviews help build professional's reputation
            - Your behavior (cancellations, no-shows) affects your "reliability score"
            
            PRIORITY BOOKING:
            - Premium bookings: ₹X extra to skip regular queue (+30% of service price)
            - Emergency booking: For Healthcare sector only, immediate queue position
            - SLA: Premium appointments guaranteed within 15 minutes of scheduled time
            
            COMMON ISSUES:
            - Q: "How do I join a virtual appointment?"
              A: Use the meeting token provided at booking time. Visit /meeting/join and enter your token.
            
            - Q: "Can I change my mind after booking?"
              A: Yes, you can cancel or reschedule 24 hours before appointment time.
            
            - Q: "What if the professional is late?"
              A: Premium bookings get escalated after 30 minutes. Regular bookings show estimated wait time.
            
            - Q: "How is my reliability score calculated?"
              A: Based on completed appointments, cancellations, no-shows, and reviews given.
            """;
    }
    
    private String getProfessionalContext(UUID professionalId) {
        try {
            var professional = professionalService.getProfessionalById(professionalId);
            if (professional == null) {
                return "Professional not found.";
            }
            
            return String.format("""
                Current Professional Context:
                Name: %s
                Specialty: %s
                Sector: %s
                Rating: %.1f stars
                Accepts Bookings: %s
                """,
                professional.getDisplayName(),
                professional.getSpecialty() != null ? professional.getSpecialty() : "N/A",
                professional.getSector(),
                professional.getRatingAvg() != null ? professional.getRatingAvg() : 0.0,
                professional.isAcceptingBookings() ? "Yes" : "No"
            );
        } catch (Exception e) {
            log.error("Error building professional context", e);
            return "Professional context unavailable.";
        }
    }
    
    private String getBookingContext(UUID appointmentId) {
        try {
            // Placeholder: would fetch actual appointment details
            return String.format("""
                Current Booking Context:
                Appointment ID: %s
                You can ask about: booking status, how to join meeting, payment details, cancellation policy
                """, appointmentId);
        } catch (Exception e) {
            log.error("Error building booking context", e);
            return "Booking context unavailable.";
        }
    }
    
    private String getUserBookingContext(UUID userId) {
        try {
            // Placeholder: would fetch user's upcoming appointments
            return """
                Your Upcoming Appointments:
                (User's recent/upcoming appointments would be listed here)
                Ask me about: booking status, how to join virtual meetings, payment information
                """;
        } catch (Exception e) {
            log.error("Error building user booking context", e);
            return "No upcoming appointments.";
        }
    }
}
