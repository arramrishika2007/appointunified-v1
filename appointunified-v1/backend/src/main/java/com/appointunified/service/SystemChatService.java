package com.appointunified.service;

import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import com.appointunified.dto.response.SystemChatResponse;
import com.appointunified.entity.Appointment;
import com.appointunified.enums.UserRole;
import com.appointunified.entity.User;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.ProfessionalRepository;
import com.appointunified.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SystemChatService {
    
    private final GroqService groqService;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    
    /**
     * Answer a user's question about the platform using RAG + Groq API
     */
    @Transactional
    public SystemChatResponse answerQuestion(User user, String question, String contextType, UUID contextId) {
        try {
            String normalizedContextType = contextType == null ? "general" : contextType.toLowerCase(Locale.ROOT);
            String ragContext = buildRAGContext(user, normalizedContextType, contextId);

            if (!groqService.isConfigured()) {
                log.warn("Groq API key is not configured; serving secure local response for user={}, role={}", user.getId(), user.getRole());
                String local = generateLocalRoleBasedResponse(user, question, ragContext, normalizedContextType, contextId);
                return new SystemChatResponse(
                    question,
                    local,
                    "SUCCESS"
                );
            }
            
            // Call Groq API with context
            String response = groqService.generateChatResponse(question, ragContext);
            
            if (response.isBlank()) {
                log.warn("Groq API returned empty response for question: {}", question);
                String fallback = generateLocalRoleBasedResponse(user, question, ragContext, normalizedContextType, contextId);
                return new SystemChatResponse(question, fallback, "SUCCESS");
            }
            
            log.info("Generated chat response for user={}, role={}, contextType={}", user.getId(), user.getRole(), normalizedContextType);
            return new SystemChatResponse(question, response, "SUCCESS");
            
        } catch (Exception e) {
            log.error("Error answering system chat question", e);
            String safeFallback = generateLocalRoleBasedResponse(user, question, "", contextType, contextId);
            return new SystemChatResponse(
                question,
                safeFallback,
                "SUCCESS");
        }
    }
    
    // ==================== Private Helper Methods ====================
    
    private String buildRAGContext(User user, String contextType, UUID contextId) {
        StringBuilder context = new StringBuilder();
        
        // Common platform knowledge
        context.append(getPlatformKnowledge());
        context.append("\n\n").append(getRoleSecurityPolicy(user));
        
        // Context-specific information
        if ("professional".equals(contextType) && contextId != null) {
            context.append("\n\n").append(getProfessionalContext(user, contextId));
        } else if ("booking".equals(contextType) && contextId != null) {
            context.append("\n\n").append(getBookingContext(user, contextId));
        } else if ("general".equals(contextType)) {
            context.append("\n\n").append(getUserScopedGeneralContext(user));
        }
        
        return context.toString();
    }

    private String getRoleSecurityPolicy(User user) {
        return String.format("""
            Request Security Context:
            - Authenticated User ID: %s
            - User Role: %s
            - Data policy: reveal only role-authorized, minimum-required data from provided context.
            - Never reveal raw PII, credentials, IDs, exact address, meeting tokens, or internal/admin notes.
            - If asked for unauthorized or unavailable data, respond that access is restricted.
            """, user.getId(), user.getRole());
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
    
    private String getProfessionalContext(User requester, UUID professionalId) {
        try {
            var professional = professionalRepository.findById(professionalId).orElse(null);
            if (professional == null) {
                return "Professional not found.";
            }

            boolean isOwner = professional.getUser() != null && professional.getUser().getId().equals(requester.getId());
            boolean isAdmin = isAdminRole(requester.getRole());

            if (!isOwner && !isAdmin && requester.getRole() != UserRole.PUBLIC) {
                return "Professional context restricted for your role.";
            }
            
            String baseContext = String.format("""
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

            if (!isOwner) {
                return baseContext;
            }

            var upcoming = appointmentRepository
                .findByProfessionalIdOrderByStartTimeAsc(professionalId, PageRequest.of(0, 5))
                .getContent();

            String schedulePreview = upcoming.isEmpty()
                ? "No upcoming appointments."
                : upcoming.stream().map(this::formatAppointmentForProfessional).collect(Collectors.joining("\n"));

            return baseContext + "\nOwner Schedule Preview:\n" + schedulePreview;
        } catch (Exception e) {
            log.error("Error building professional context", e);
            return "Professional context unavailable.";
        }
    }
    
    private String getBookingContext(User requester, UUID appointmentId) {
        try {
            var appointmentOpt = appointmentRepository.findById(appointmentId);
            if (appointmentOpt.isEmpty()) {
                return "Booking not found.";
            }

            Appointment appointment = appointmentOpt.get();
            boolean isClient = appointment.getClient() != null && appointment.getClient().getId().equals(requester.getId());
            boolean isProfessional = appointment.getProfessional() != null
                && appointment.getProfessional().getUser() != null
                && appointment.getProfessional().getUser().getId().equals(requester.getId());
            boolean isAdmin = isAdminRole(requester.getRole());

            if (!isClient && !isProfessional && !isAdmin) {
                return "Booking context restricted. You can only access your own appointment data.";
            }

            String roleView = isClient
                ? formatAppointmentForPublic(appointment)
                : (isProfessional ? formatAppointmentForProfessional(appointment) : formatAppointmentForAdmin(appointment));

            return "Current Booking Context:\n" + roleView +
                "\nAllowed Q&A: status updates, schedule, payment state summary, cancellation and reschedule guidance.";
        } catch (Exception e) {
            log.error("Error building booking context", e);
            return "Booking context unavailable.";
        }
    }
    
    private String getUserScopedGeneralContext(User user) {
        try {
            if (user.getRole() == UserRole.PUBLIC) {
                var myAppointments = appointmentRepository
                    .findByClientIdOrderByStartTimeDesc(user.getId(), PageRequest.of(0, 5))
                    .getContent();

                String lines = myAppointments.isEmpty()
                    ? "No recent appointments found."
                    : myAppointments.stream().map(this::formatAppointmentForPublic).collect(Collectors.joining("\n"));

                return "Your Recent Appointments (public-safe):\n" + lines +
                    "\nAsk me about status, cancellation policy, payment state, and virtual/in-person steps.";
            }

            if (user.getRole() == UserRole.PROFESSIONAL) {
                var professional = professionalRepository.findByUserId(user.getId()).orElse(null);
                if (professional == null) {
                    return "Professional profile not found.";
                }

                var upcoming = appointmentRepository
                    .findByProfessionalIdOrderByStartTimeAsc(professional.getId(), PageRequest.of(0, 5))
                    .getContent();

                String lines = upcoming.isEmpty()
                    ? "No upcoming appointments found."
                    : upcoming.stream().map(this::formatAppointmentForProfessional).collect(Collectors.joining("\n"));

                return "Your Schedule Snapshot (professional-safe):\n" + lines +
                    "\nAsk me about queue, schedule, payment states, and appointment operations.";
            }

            long totalAppointments = appointmentRepository.count();
            long totalProfessionals = professionalRepository.count();
            long totalUsers = userRepository.count();
            return String.format("""
                Admin/SuperAdmin Analytics Snapshot (aggregated only):
                - Total users: %d
                - Total appointments: %d
                - Total professionals: %d
                Note: PII and secret fields are excluded by policy.
                """, totalUsers, totalAppointments, totalProfessionals);
        } catch (Exception e) {
            log.error("Error building user booking context", e);
            return "No upcoming appointments.";
        }
    }

    private String formatAppointmentForPublic(Appointment appointment) {
        String professionalName = appointment.getProfessional() != null ? appointment.getProfessional().getDisplayName() : "N/A";
        String serviceName = appointment.getService() != null ? appointment.getService().getName() : "N/A";
        return String.format("- Appointment %s | %s | %s | Service: %s | Professional: %s | Deposit: %s | Final: %s",
            appointment.getId(),
            appointment.getStartTime(),
            appointment.getStatus(),
            serviceName,
            professionalName,
            appointment.getDepositStatus(),
            appointment.getFinalPaymentStatus());
    }

    private String formatAppointmentForProfessional(Appointment appointment) {
        String clientLabel = "client";
        if (appointment.getClient() != null && appointment.getClient().getFullName() != null && !appointment.getClient().getFullName().isBlank()) {
            clientLabel = firstWord(appointment.getClient().getFullName());
        }

        String serviceName = appointment.getService() != null ? appointment.getService().getName() : "N/A";
        return String.format("- Appointment %s | %s | %s | Service: %s | Client: %s | Mode: %s",
            appointment.getId(),
            appointment.getStartTime(),
            appointment.getStatus(),
            serviceName,
            clientLabel,
            appointment.isVirtual() ? "virtual" : "offline");
    }

    private String formatAppointmentForAdmin(Appointment appointment) {
        String serviceName = appointment.getService() != null ? appointment.getService().getName() : "N/A";
        return String.format("- Appointment %s | %s | %s | Service: %s | Priority: %s | Mode: %s",
            appointment.getId(),
            appointment.getStartTime(),
            appointment.getStatus(),
            serviceName,
            appointment.getPriority(),
            appointment.isVirtual() ? "virtual" : "offline");
    }

    private boolean isAdminRole(UserRole role) {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN;
    }

    private String firstWord(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "client";
        }
        int idx = fullName.trim().indexOf(' ');
        return idx < 0 ? fullName.trim() : fullName.trim().substring(0, idx);
    }

    private String generateLocalRoleBasedResponse(User user, String question, String ragContext, String contextType, UUID contextId) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        String appointmentSnapshots = ragContext.lines()
            .filter(line -> line.startsWith("- Appointment"))
            .limit(5)
            .collect(Collectors.joining("\n"));
        long appointmentCount = ragContext.lines().filter(line -> line.startsWith("- Appointment")).count();
        String firstAppointmentLine = ragContext.lines()
            .filter(line -> line.startsWith("- Appointment"))
            .findFirst()
            .orElse("");

        boolean asksJoin = q.contains("join") || q.contains("meet") || q.contains("meeting") || q.contains("virtual") || q.contains("online");
        boolean asksBooking = q.equals("booking") || q.equals("bookings") || q.contains("show booking") || q.contains("my booking") || q.contains("appointment list");
        boolean asksCancel = q.contains("cancel");
        boolean asksReschedule = q.contains("reschedule") || q.contains("change time") || q.contains("move appointment");
        boolean asksPayment = q.contains("payment") || q.contains("deposit") || q.contains("upi") || q.contains("final amount");
        boolean asksQueue = q.contains("queue") || q.contains("wait") || q.contains("token") || q.contains("position");
        boolean asksHowManyUsers = (q.contains("how many") && q.contains("user"))
            || q.contains("how many user")
            || q.contains("users in this system")
            || q.contains("total users")
            || q.contains("number of users");

        if (asksHowManyUsers) {
            String usersLine = ragContext.lines()
                .filter(line -> line.toLowerCase(Locale.ROOT).contains("total users"))
                .findFirst()
                .orElse("");

            if (!usersLine.isBlank()) {
                return usersLine.replace("- ", "");
            }

            return "Access denied. Admin only.";
        }

        if (asksJoin) {
            String joinHint = "To join: Dashboard > Appointments > open your virtual appointment > click Join Meeting > enter the meeting token shown in the app.";
            if (!firstAppointmentLine.isBlank()) {
                return "I can help you join right now.\n"
                    + "Next appointment I can see: " + firstAppointmentLine + "\n"
                    + joinHint + "\n"
                    + "If you want, say: queue status, payment status, cancel, or reschedule.";
            }

            return "I can help you join a virtual meet, but I do not see a recent booking in your current context.\n"
                + joinHint + "\n"
                + "If you share a booking context, I can guide step-by-step.";
        }

        if (q.contains("how will this system work") || q.contains("how does this system work") || q.contains("how this system work") || q.equals("how will this system work")) {
            return "Find professional > Book slot > Pay deposit > Attend > Pay final amount";
        }

        if (q.contains("explain about this") || q.equals("explain") || q.equals("explain this") || q.contains("explain this system")) {
            return "Appointment booking platform with professionals, 2-stage payments, virtual meetings, and real-time queue tracking.";
        }

        if (asksBooking) {
            if (!appointmentSnapshots.isBlank()) {
                return "I found " + appointmentCount + " recent bookings in your role-filtered data.\n"
                    + appointmentSnapshots + "\n"
                    + "Tell me what you want next for a booking: join virtual, cancel, reschedule, payment, or queue status.";
            }

            return "I do not see recent bookings in your current context.\n"
                + "You can ask: book appointment, join virtual, cancel, reschedule, payment, or queue status.";
        }

        if (asksCancel) {
            if (!firstAppointmentLine.isBlank()) {
                return "For cancellation, open the appointment and choose Cancel. Policy allows cancellation up to 24 hours before start time (fees may apply).\n"
                    + "Booking in context: " + firstAppointmentLine + "\n"
                    + "If you want, I can also explain reschedule steps for the same booking.";
            }
            return "You can cancel from Appointment details > Cancel, usually up to 24 hours before start time (fees may apply).";
        }

        if (asksReschedule) {
            if (!firstAppointmentLine.isBlank()) {
                return "For reschedule, open the appointment > Reschedule > choose an available slot > confirm.\n"
                    + "Booking in context: " + firstAppointmentLine + "\n"
                    + "If no slots appear, ask me for booking options and I will help pick next steps.";
            }
            return "To reschedule: Appointment details > Reschedule > choose new slot > confirm.";
        }

        if (asksPayment) {
            if (!appointmentSnapshots.isBlank()) {
                return "Payment is split into two stages: deposit before appointment and final payment after completion.\n"
                    + "Current payment snapshot:\n" + appointmentSnapshots + "\n"
                    + "Say 'payment for booking' with a booking context and I will explain exact next action.";
            }
            return "Payment flow: pay deposit before appointment, then final payment after appointment completion.";
        }

        if (asksQueue) {
            if (!firstAppointmentLine.isBlank()) {
                return "Queue status is available from Appointment details. If token/position is assigned, it updates in real time.\n"
                    + "Booking in context: " + firstAppointmentLine + "\n"
                    + "You can ask me: 'show queue status for my booking'.";
            }
            return "Queue status appears in Appointment details with token number, position, and estimated wait.";
        }

        if (q.contains("appointment") || q.contains("status") || q.contains("schedule")) {
            if (!appointmentSnapshots.isBlank()) {
                return "I can work with your live appointment data.\n"
                    + appointmentSnapshots + "\n"
                    + "Tell me one action: join virtual, cancel, reschedule, payment, or queue status.";
            }
        }

        return generateFallbackResponse(question, user, contextType, contextId);
    }

    private String generateFallbackResponse(String question, User user, String contextType, UUID contextId) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);

        if (q.contains("what is this app") || q.contains("what was this app") || q.contains("what is appointunified") || q.contains("about this app")) {
            return "Appointment scheduling app for Healthcare, Government, Services with verified professionals.";
        }

        if (q.contains("book") || q.contains("appointment") || q.contains("how do i use")) {
            return "Select sector > Find professional > Choose slot > Pick online/offline > Confirm > Pay deposit";
        }

        if (q.contains("cancel") || q.contains("reschedule")) {
            return "Cancel/reschedule within 24 hours of appointment. Go to appointment > Cancel/Reschedule";
        }

        return "I can answer from your live, role-filtered data. Ask one of these: booking, join virtual, cancel, reschedule, payment, queue status, or analytics.";
    }
}
