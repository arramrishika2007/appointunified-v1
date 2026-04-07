package com.appointunified.service;

import com.appointunified.entity.Appointment;
import com.appointunified.entity.Professional;
import com.appointunified.entity.NotificationPreference;
import com.appointunified.entity.User;
import com.appointunified.entity.WorkflowDefinition;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.NotificationPreferenceRepository;
import com.appointunified.repository.UserDeviceRepository;
import com.appointunified.repository.UserRepository;
import com.appointunified.enums.UserRole;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.net.URLEncoder.encode;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final AppointmentRepository appointmentRepository;
    private final JavaMailSender mailSender;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    private final HttpClient httpClient = HttpClient.newHttpClient();

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

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("EEE, MMM d yyyy 'at' h:mm a");

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
    @Transactional(readOnly = true)
    public void sendBookingConfirmation(java.util.UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        User client = appointment.getClient();
        if (client.getEmail() == null && client.getPhone() == null) return;
        try {
            String formattedTime = appointment.getStartTime().format(FORMATTER);
            String shareUrl = frontendUrl + "/appointments/share/" + appointment.getShareToken();

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
        } catch (Exception e) {
            log.error("Failed to send booking confirmation: {}", e.getMessage());
        }
    }

    @Async
    @Transactional(readOnly = true)
    public void sendCancellationNotification(java.util.UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        User client = appointment.getClient();
        if (client.getEmail() == null && client.getPhone() == null) return;
        try {
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
        } catch (Exception e) {
            log.error("Failed to send cancellation notification: {}", e.getMessage());
        }
    }

    @Async
    @Transactional(readOnly = true)
    public void sendRescheduleNotification(java.util.UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        User client = appointment.getClient();
        if (client.getEmail() == null && client.getPhone() == null) return;
        try {
            String formattedTime = appointment.getStartTime().format(FORMATTER);
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.debug("Email sent to {} — Subject: {}", user.getEmail(), subject);
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
        return preferenceRepository.findByUserId(user.getId()).orElseGet(() -> NotificationPreference.builder()
                .user(user)
                .emailEnabled(true)
                .smsEnabled(true)
                .whatsappEnabled(false)
                .pushEnabled(true)
                .reminderHours((short) 24)
                .build());
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
