package com.appointunified.service;

import com.appointunified.entity.Appointment;
import com.appointunified.entity.Professional;
import com.appointunified.entity.NotificationPreference;
import com.appointunified.entity.User;
import com.appointunified.entity.WorkflowDefinition;
import com.appointunified.dto.response.NotificationResponse;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.NotificationRepository;
import com.appointunified.repository.NotificationPreferenceRepository;
import com.appointunified.repository.UserDeviceRepository;
import com.appointunified.repository.UserRepository;
import com.appointunified.enums.UserRole;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.net.URLEncoder.encode;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public NotificationService(
            AppointmentRepository appointmentRepository,
            NotificationRepository notificationRepository,
            JavaMailSender mailSender,
            NotificationPreferenceRepository preferenceRepository,
            UserDeviceRepository userDeviceRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            ObjectProvider<FirebaseMessaging> firebaseMessagingProvider) {
        this.appointmentRepository = appointmentRepository;
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
        this.preferenceRepository = preferenceRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.firebaseMessagingProvider = firebaseMessagingProvider;
    }

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.frontend.url:${FRONTEND_URL:${APP_BASE_URL:http://localhost:3000}}}")
    private String frontendUrl;

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${RESEND_FROM_EMAIL:}")
    private String resendFromEmail;

    @Value("${TWILIO_ACCOUNT_SID:}")
    private String twilioAccountSid;

    @Value("${TWILIO_AUTH_TOKEN:}")
    private String twilioAuthToken;

    @Value("${TWILIO_FROM_NUMBER:}")
    private String twilioFromNumber;

    @Value("${SUPABASE_URL:}")
    private String supabaseUrl;

    @Value("${SUPABASE_SERVICE_ROLE_KEY:}")
    private String supabaseServiceRoleKey;

    @Value("${app.supabase.notifications-table:notification_events}")
    private String supabaseNotificationsTable;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("EEE, MMM d yyyy 'at' h:mm a");

    @Transactional
    public com.appointunified.entity.Notification createNotification(
            UUID userId,
            String type,
            String title,
            String message,
            String actionUrl) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Cannot create notification; user not found: {}", userId);
            return null;
        }

        com.appointunified.entity.Notification.NotificationType parsedType;
        try {
            parsedType = com.appointunified.entity.Notification.NotificationType.valueOf(type.toUpperCase());
        } catch (Exception ignored) {
            parsedType = com.appointunified.entity.Notification.NotificationType.SYSTEM;
        }

        com.appointunified.entity.Notification notification = new com.appointunified.entity.Notification(
                user,
                parsedType,
                title,
                message,
                actionUrl
        );
        com.appointunified.entity.Notification saved = notificationRepository.save(notification);
        mirrorNotificationToSupabase(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        return notificationRepository.findByUserIdAndIsArchivedFalseOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalseAndIsArchivedFalse(userId);
    }

    @Transactional
    public void markNotificationAsRead(UUID notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Transactional
    public void markAllNotificationsAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional(readOnly = true)
    public java.util.List<NotificationResponse> getRecentNotifications(UUID userId, int days) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(days);
        return notificationRepository.findByUserIdAndIsArchivedFalseAndCreatedAtAfterOrderByCreatedAtDesc(userId, cutoff)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(com.appointunified.entity.Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getActionUrl(),
                notification.getIsRead(),
                notification.getIsArchived(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }

    @Async
    public void sendWelcomeEmail(User user) {
        if (user.getEmail() == null) return;
        try {
            String subject = "Welcome to AppointUnified!";
            String body = String.format("""
                    Hi %s,
                    
                    Welcome to AppointUnified — your smart appointment engine for Healthcare, Government, and Services.
                    
                    You can now:
                    • Browse and book verified professionals
                    • Track your appointments in real-time
                    • Manage all your bookings in one place
                    
                    Get started: %s
                    
                    — The AppointUnified Team
                    """, user.getFullName(), frontendUrl);
            sendEmail(user, subject, body);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Async
    @Transactional
    public void sendBookingConfirmation(java.util.UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            log.warn("Skipping booking confirmation; appointment not found: {}", appointmentId);
            return;
        }
        User client = appointment.getClient();
        User professionalUser = appointment.getProfessional() != null ? appointment.getProfessional().getUser() : null;
        if (client.getEmail() == null && client.getPhone() == null) return;
        try {
            String formattedTime = appointment.getStartTime().format(FORMATTER);
            String shareUrl = frontendUrl + "/appointments/share/" + appointment.getShareToken();
            String bookingPath = "/dashboard/bookings/" + appointment.getId();
            String proPath = "/professional/dashboard";

            createNotification(
                    client.getId(),
                    "APPOINTMENT",
                    "Appointment confirmed",
                    appointment.getService().getName() + " with " + appointment.getProfessional().getDisplayName() + " on " + formattedTime,
                    bookingPath
            );

            if (professionalUser != null) {
                createNotification(
                        professionalUser.getId(),
                        "APPOINTMENT",
                        "New appointment booked",
                        "A client booked " + appointment.getService().getName() + " on " + formattedTime,
                        proPath
                );
            }

            String subject = "✅ Appointment Confirmed — " + appointment.getService().getName();
            String body = String.format("""
                    Hi %s,
                    
                    Your appointment is confirmed!
                    
                    📋 Details:
                    ─────────────────────────────
                    Service:      %s
                    Provider:     %s
                    Date & Time:  %s
                    Duration:     %d minutes
                    Type:         %s
                    ─────────────────────────────
                    
                    Appointment ID: %s
                    
                    📅 Add to Calendar: %s/appointments/%s/ical
                    🔗 Share this appointment: %s
                    
                    You can manage your bookings at: %s/dashboard/bookings
                    
                    See you soon!
                    — AppointUnified
                    """,
                    client.getFullName(),
                    appointment.getService().getName(),
                    appointment.getProfessional().getDisplayName(),
                    formattedTime,
                    appointment.getService().getDurationMinutes(),
                    appointment.isVirtual() ? "Online/Virtual" : "In-person",
                    appointment.getId(),
                    frontendUrl, appointment.getId(),
                    shareUrl,
                    frontendUrl);

            sendEmail(client, subject, body);
            sendPushNotification(client, "Appointment confirmed", appointment.getService().getName() + " with " + appointment.getProfessional().getDisplayName() + " on " + formattedTime);
            sendChannelMessage(client, String.format(
                    "Appointment confirmed: %s with %s on %s",
                    appointment.getService().getName(),
                    appointment.getProfessional().getDisplayName(),
                    formattedTime));

            if (professionalUser != null) {
                sendPushNotification(professionalUser, "New booking", appointment.getService().getName() + " on " + formattedTime);
                sendChannelMessage(professionalUser, String.format(
                        "New booking: %s at %s",
                        appointment.getService().getName(),
                        formattedTime));
            }
        } catch (Exception e) {
            log.error("Failed to send booking confirmation: {}", e.getMessage());
        }
    }

    @Async
    @Transactional
    public void sendCancellationNotification(java.util.UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            log.warn("Skipping cancellation notification; appointment not found: {}", appointmentId);
            return;
        }
        User client = appointment.getClient();
        User professionalUser = appointment.getProfessional() != null ? appointment.getProfessional().getUser() : null;
        if (client.getEmail() == null && client.getPhone() == null) return;
        try {
            String bookingPath = "/dashboard/bookings/" + appointment.getId();
            String proPath = "/professional/dashboard";

            createNotification(
                    client.getId(),
                    "APPOINTMENT",
                    "Appointment cancelled",
                    "Your appointment for " + appointment.getService().getName() + " was cancelled.",
                    bookingPath
            );

            if (professionalUser != null) {
                createNotification(
                        professionalUser.getId(),
                        "APPOINTMENT",
                        "Appointment cancelled",
                        "A client cancelled " + appointment.getService().getName() + ".",
                        proPath
                );
            }

            String subject = "❌ Appointment Cancelled — " + appointment.getService().getName();
            String body = String.format("""
                    Hi %s,
                    
                    Your appointment has been cancelled.
                    
                    Service:  %s
                    Provider: %s
                    
                    %s
                    
                    You can book a new appointment at: %s
                    
                    — AppointUnified
                    """,
                    client.getFullName(),
                    appointment.getService().getName(),
                    appointment.getProfessional().getDisplayName(),
                    appointment.getCancellationReason() != null
                            ? "Reason: " + appointment.getCancellationReason() : "",
                    frontendUrl);

            sendEmail(client, subject, body);
            sendPushNotification(client, "Appointment cancelled", "Your appointment for " + appointment.getService().getName() + " was cancelled.");
            sendChannelMessage(client, String.format(
                    "Your appointment for %s was cancelled.",
                    appointment.getService().getName()));

            if (professionalUser != null) {
                sendPushNotification(professionalUser, "Appointment cancelled", appointment.getService().getName() + " was cancelled by client.");
                sendChannelMessage(professionalUser, "Booking cancelled by client.");
            }
        } catch (Exception e) {
            log.error("Failed to send cancellation notification: {}", e.getMessage());
        }
    }

    @Async
    @Transactional
    public void sendRescheduleNotification(java.util.UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
        if (appointment == null) {
            log.warn("Skipping reschedule notification; appointment not found: {}", appointmentId);
            return;
        }
        User client = appointment.getClient();
        User professionalUser = appointment.getProfessional() != null ? appointment.getProfessional().getUser() : null;
        if (client.getEmail() == null && client.getPhone() == null) return;
        try {
            String formattedTime = appointment.getStartTime().format(FORMATTER);
            String bookingPath = "/dashboard/bookings/" + appointment.getId();
            String proPath = "/professional/dashboard";

            createNotification(
                    client.getId(),
                    "APPOINTMENT",
                    "Appointment rescheduled",
                    "Your " + appointment.getService().getName() + " appointment moved to " + formattedTime + ".",
                    bookingPath
            );

            if (professionalUser != null) {
                createNotification(
                        professionalUser.getId(),
                        "APPOINTMENT",
                        "Appointment rescheduled",
                        "An appointment was moved to " + formattedTime + ".",
                        proPath
                );
            }

            String subject = "🔄 Appointment Rescheduled — " + appointment.getService().getName();
            String body = String.format("""
                    Hi %s,
                    
                    Your appointment has been rescheduled.
                    
                    Service:      %s
                    Provider:     %s
                    New Time:     %s
                    
                    Manage at: %s/dashboard/bookings
                    
                    — AppointUnified
                    """,
                    client.getFullName(),
                    appointment.getService().getName(),
                    appointment.getProfessional().getDisplayName(),
                    formattedTime,
                    frontendUrl);

            sendEmail(client, subject, body);
            sendPushNotification(client, "Appointment rescheduled", "Your appointment for " + appointment.getService().getName() + " was rescheduled to " + formattedTime + ".");
            sendChannelMessage(client, String.format(
                    "Your appointment for %s was rescheduled to %s.",
                    appointment.getService().getName(),
                    formattedTime));

            if (professionalUser != null) {
                sendPushNotification(professionalUser, "Appointment rescheduled", "A booking moved to " + formattedTime + ".");
                sendChannelMessage(professionalUser, "A booking was rescheduled.");
            }
        } catch (Exception e) {
            log.error("Failed to send reschedule notification: {}", e.getMessage());
        }
    }

    // NEW V1 FEATURE 5: Send appointment reminder (called by scheduler)
    @Async
    @Transactional(readOnly = true)
    public void sendAppointmentReminder(java.util.UUID appointmentId, int hoursAhead) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        User client = appointment.getClient();
        if (client.getEmail() == null && client.getPhone() == null) return;
        try {
            String formattedTime = appointment.getStartTime().format(FORMATTER);
            String subject = String.format("⏰ Reminder: Appointment in %d hour%s",
                    hoursAhead, hoursAhead == 1 ? "" : "s");
            String body = String.format("""
                    Hi %s,
                    
                    Just a reminder — your appointment is coming up!
                    
                    Service:  %s
                    Provider: %s
                    Time:     %s
                    %s
                    
                    %s
                    
                    — AppointUnified
                    """,
                    client.getFullName(),
                    appointment.getService().getName(),
                    appointment.getProfessional().getDisplayName(),
                    formattedTime,
                    appointment.isVirtual() ? "Meeting Link: " + appointment.getMeetLink() : "",
                    "View details: " + frontendUrl + "/dashboard/bookings");

            sendEmail(client, subject, body);
                sendPushNotification(client, "Appointment reminder", String.format("%s with %s in %d hour%s", appointment.getService().getName(), appointment.getProfessional().getDisplayName(), hoursAhead, hoursAhead == 1 ? "" : "s"));
            sendChannelMessage(client, String.format(
                    "Reminder: %s with %s in %d hour%s",
                    appointment.getService().getName(),
                    appointment.getProfessional().getDisplayName(),
                    hoursAhead, hoursAhead == 1 ? "" : "s"));
        } catch (Exception e) {
            log.error("Failed to send reminder: {}", e.getMessage());
        }
    }

    @Async
    public void sendWaitlistSpotAvailable(User user, Appointment cancelledAppointment) {
        if (user.getEmail() == null && user.getPhone() == null) return;
        try {
            createNotification(
                user.getId(),
                "WAITLIST",
                "Waitlist spot available",
                "A slot opened for " + cancelledAppointment.getService().getName() + ". Book now.",
                "/provider/" + cancelledAppointment.getProfessional().getId()
            );

            String subject = "A spot just opened for " + cancelledAppointment.getService().getName();
            String body = String.format("""
                    Hi %s,

                    Good news. A waitlist spot just opened up.

                    Provider: %s
                    Service:  %s
                    New slot: %s

                    Please book soon before it gets taken.
                    Book now: %s/provider/%s

                    — AppointUnified
                    """,
                    user.getFullName(),
                    cancelledAppointment.getProfessional().getDisplayName(),
                    cancelledAppointment.getService().getName(),
                    cancelledAppointment.getStartTime().format(FORMATTER),
                    frontendUrl,
                    cancelledAppointment.getProfessional().getId());

            sendEmail(user, subject, body);
            sendPushNotification(user, "Waitlist update", "A slot opened for " + cancelledAppointment.getService().getName());
            sendChannelMessage(user, String.format(
                    "Waitlist update: Slot opened for %s with %s at %s",
                    cancelledAppointment.getService().getName(),
                    cancelledAppointment.getProfessional().getDisplayName(),
                    cancelledAppointment.getStartTime().format(FORMATTER)));
        } catch (Exception e) {
            log.error("Failed to send waitlist promotion notification: {}", e.getMessage());
        }
    }

    @Async
    public void sendWorkflowNextStepPrompt(User user,
                                           WorkflowDefinition workflow,
                                           int nextStepOrder,
                                           Appointment completedAppointment) {
        if (user.getEmail() == null && user.getPhone() == null) return;
        try {
            createNotification(
                user.getId(),
                "SYSTEM",
                "Workflow advanced",
                "Step " + nextStepOrder + " is ready to book in workflow " + workflow.getName() + ".",
                "/dashboard/workflows"
            );

            String subject = "Next step ready in workflow: " + workflow.getName();
            String body = String.format("""
                    Hi %s,

                    Your workflow "%s" has advanced to step %d.
                    Please book your next step to continue.

                    Previous completed appointment:
                    %s with %s

                    Continue here: %s/bookings

                    — AppointUnified
                    """,
                    user.getFullName(),
                    workflow.getName(),
                    nextStepOrder,
                    completedAppointment.getService().getName(),
                    completedAppointment.getProfessional().getDisplayName(),
                    frontendUrl);

            sendEmail(user, subject, body);
            sendPushNotification(user, "Workflow advanced", "Step " + nextStepOrder + " is ready to book.");
            sendChannelMessage(user, "Workflow update: your next step is ready to book.");
        } catch (Exception e) {
            log.error("Failed to send workflow next-step prompt: {}", e.getMessage());
        }
    }

    @Async
    public void notifySuperAdminsProfessionalApproved(Professional professional, User admin) {
        if (professional == null || admin == null) {
            return;
        }

        var superAdmins = userRepository.findAllByRole(UserRole.SUPER_ADMIN);
        if (superAdmins.isEmpty()) {
            log.info("No super admins found for approval notification of professional {}", professional.getId());
            return;
        }

        String subject = "Professional approved by admin: " + professional.getDisplayName();
        String body = String.format("""
                Hello,

                The professional profile below was approved by an admin:

                Professional: %s
                Sector: %s
                Approved by: %s
                Status: %s

                You can review the activity in the super-admin panel.

                — AppointUnified
                """,
                professional.getDisplayName(),
                professional.getSector() != null ? professional.getSector().name() : "UNKNOWN",
                admin.getFullName() != null ? admin.getFullName() : admin.getPhone(),
                professional.getVerificationStatus() != null ? professional.getVerificationStatus().name() : "UNKNOWN");

        for (User superAdmin : superAdmins) {
            try {
            createNotification(
                superAdmin.getId(),
                "VERIFICATION",
                subject,
                "Professional " + professional.getDisplayName() + " was approved by "
                    + (admin.getFullName() != null ? admin.getFullName() : admin.getPhone()) + ".",
                "/super-admin/verifications"
            );
                sendEmail(superAdmin, subject, body);
                sendPushNotification(superAdmin, subject, body);
            } catch (Exception ex) {
                log.warn("Failed to notify super admin {} about approval of professional {}: {}",
                        superAdmin.getId(), professional.getId(), ex.getMessage());
            }
        }
    }

    private void sendEmail(User user, String subject, String body) {
        if (!isEmailEnabled(user)) return;

        if (isResendConfigured()) {
            sendViaResend(user.getEmail(), subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.debug("Email sent to {} — Subject: {}", user.getEmail(), subject);
        } catch (Exception e) {
            log.warn("Email delivery failed for {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void sendChannelMessage(User user, String message) {
        NotificationPreference prefs = getPreferences(user);
        if (prefs == null || user.getPhone() == null) return;

        if (prefs.isWhatsappEnabled()) {
            sendTwilioMessage(user.getPhone(), message, true);
        } else if (prefs.isSmsEnabled()) {
            sendTwilioMessage(user.getPhone(), message, false);
        }
    }

    private NotificationPreference getPreferences(User user) {
        return preferenceRepository.findByUserId(user.getId()).orElseGet(() -> {
            NotificationPreference defaults = new NotificationPreference();
            defaults.setUser(user);
            defaults.setEmailEnabled(true);
            defaults.setSmsEnabled(true);
            defaults.setWhatsappEnabled(false);
            defaults.setPushEnabled(true);
            defaults.setReminderHours((short) 24);
            return defaults;
        });
    }

    private boolean isEmailEnabled(User user) {
        NotificationPreference prefs = getPreferences(user);
        return prefs != null && prefs.isEmailEnabled() && user.getEmail() != null;
    }

    private boolean isResendConfigured() {
        return resendApiKey != null && !resendApiKey.isBlank()
                && resendFromEmail != null && !resendFromEmail.isBlank();
    }

    private void sendViaResend(String to, String subject, String body) {
        try {
            String payload = objectMapper.writeValueAsString(java.util.Map.of(
                    "from", resendFromEmail,
                    "to", new String[]{to},
                    "subject", subject,
                    "text", body
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("Resend email sent to {} — Subject: {}", to, subject);
            } else {
                log.warn("Resend API returned {} for {}: {}", response.statusCode(), to, response.body());
            }
        } catch (Exception e) {
            log.error("Resend email failed for {}: {}", to, e.getMessage());
        }
    }

    private void sendTwilioMessage(String toNumber, String body, boolean whatsapp) {
        if (twilioAccountSid == null || twilioAccountSid.isBlank()
                || twilioAuthToken == null || twilioAuthToken.isBlank()
                || twilioFromNumber == null || twilioFromNumber.isBlank()) {
            return;
        }

        try {
            String from = whatsapp ? "whatsapp:" + twilioFromNumber : twilioFromNumber;
            String to = whatsapp ? "whatsapp:" + toNumber : toNumber;
            String form = "From=" + encode(from, StandardCharsets.UTF_8)
                    + "&To=" + encode(to, StandardCharsets.UTF_8)
                    + "&Body=" + encode(body, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json"))
                    .header("Authorization", basicAuth(twilioAccountSid, twilioAuthToken))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.debug("Twilio {} sent to {}", whatsapp ? "WhatsApp" : "SMS", toNumber);
            } else {
                log.warn("Twilio returned {} for {}: {}", response.statusCode(), toNumber, response.body());
            }
        } catch (Exception e) {
            log.error("Twilio message failed for {}: {}", toNumber, e.getMessage());
        }
    }

    private String basicAuth(String username, String password) {
        String token = java.util.Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private void mirrorNotificationToSupabase(com.appointunified.entity.Notification notification) {
        if (!isSupabaseMirrorConfigured()) {
            return;
        }

        try {
            String baseUrl = supabaseUrl.endsWith("/")
                    ? supabaseUrl.substring(0, supabaseUrl.length() - 1)
                    : supabaseUrl;
            String endpoint = baseUrl + "/rest/v1/" + supabaseNotificationsTable;

            String payload = objectMapper.writeValueAsString(java.util.Map.of(
                    "notification_id", notification.getId().toString(),
                    "user_id", notification.getUser().getId().toString(),
                    "type", notification.getType().name(),
                    "title", notification.getTitle(),
                    "message", notification.getMessage() != null ? notification.getMessage() : "",
                    "action_url", notification.getActionUrl() != null ? notification.getActionUrl() : "",
                    "created_at", notification.getCreatedAt().toString()
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("apikey", supabaseServiceRoleKey)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseServiceRoleKey)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=minimal")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Supabase mirror failed for notification {}: HTTP {}", notification.getId(), response.statusCode());
            }
        } catch (Exception ex) {
            log.warn("Supabase mirror failed for notification {}: {}", notification.getId(), ex.getMessage());
        }
    }

    private boolean isSupabaseMirrorConfigured() {
        return supabaseUrl != null && !supabaseUrl.isBlank()
                && supabaseServiceRoleKey != null && !supabaseServiceRoleKey.isBlank();
    }

    private void sendPushNotification(User user, String title, String body) {
        NotificationPreference prefs = getPreferences(user);
        if (prefs == null || !prefs.isPushEnabled()) {
            return;
        }

        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            return;
        }

        var devices = userDeviceRepository.findByUserIdAndEnabledTrue(user.getId());
        if (devices.isEmpty()) {
            return;
        }

        for (var device : devices) {
            try {
                Message message = Message.builder()
                        .setToken(device.getFcmToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build();
                firebaseMessaging.send(message);
                log.debug("Push notification sent to {}", user.getId());
            } catch (Exception e) {
                log.warn("Failed to send push notification to {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}
