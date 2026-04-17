package com.appointunified.service;

import com.appointunified.entity.Appointment;
import com.appointunified.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * V3 notification extension — queue-specific alerts.
 * Add these methods by extending or adding to your existing NotificationService.
 * This class can be autowired alongside NotificationService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceV3Extension {

    private final JavaMailSender mailSender;
    private final NotificationService notificationService;

    @Value("${app.mail.from}")      private String fromEmail;
    @Value("${app.mail.from-name}") private String fromName;
    @Value("${app.frontend.url:http://localhost:3000}") private String frontendUrl;

    /** Called when a professional calls the next client */
    @Async
    public void sendQueueCallNotification(Appointment appointment) {
        User client = appointment.getClient();
        try {
            notificationService.createNotification(
                    client.getId(),
                    "QUEUE",
                    "It's your turn",
                    appointment.getProfessional().getDisplayName() + " is ready for you now.",
                    "/dashboard/bookings/" + appointment.getId()
            );

            if (client.getEmail() == null) return;

            String body = String.format("""
                Hi %s,

                🔔 It's your turn! %s is ready for you now.

                Service: %s

                Please proceed to the consultation room immediately.
                If you need more time, please inform the receptionist.

                View your appointment: %s/dashboard/bookings

                — AppointUnified Queue
                """,
                client.getFullName(),
                appointment.getProfessional().getDisplayName(),
                appointment.getService().getName(),
                frontendUrl);

            send(client.getEmail(), "🔔 Your turn now — " + appointment.getProfessional().getDisplayName(), body);
        } catch (Exception e) {
            log.error("Queue call notification failed: {}", e.getMessage());
        }
    }

    /** Called when a delay is triggered — notifies all affected clients */
    @Async
    public void sendDelayNotification(Appointment appointment, int delayMinutes, int newEstimatedWaitMins) {
        User client = appointment.getClient();
        try {
            notificationService.createNotification(
                    client.getId(),
                    "QUEUE",
                    "Queue delayed",
                    String.format("Delay: %d min. New ETA: %d min.", delayMinutes, newEstimatedWaitMins),
                    "/dashboard/bookings/" + appointment.getId()
            );

            if (client.getEmail() == null) return;

            String body = String.format("""
                Hi %s,

                ⏰ Queue update from %s:

                There's been a delay of %d minutes. Your new estimated wait time is approximately %d minutes.

                We apologise for the inconvenience. You can track your live position here:
                %s/dashboard/bookings

                — AppointUnified Queue
                """,
                client.getFullName(),
                appointment.getProfessional().getDisplayName(),
                delayMinutes,
                newEstimatedWaitMins,
                frontendUrl);

            send(client.getEmail(), "⏰ Queue delay — " + delayMinutes + " min — " + appointment.getProfessional().getDisplayName(), body);
        } catch (Exception e) {
            log.error("Delay notification failed: {}", e.getMessage());
        }
    }

    /** Called when the queue is paused */
    @Async
    public void sendQueuePausedNotification(Appointment appointment, String reason) {
        User client = appointment.getClient();
        try {
            String details = (reason == null || reason.isBlank()) ? "" : " Reason: " + reason;
            notificationService.createNotification(
                    client.getId(),
                    "QUEUE",
                    "Queue paused",
                    "The queue was temporarily paused." + details,
                    "/dashboard/bookings/" + appointment.getId()
            );

            if (client.getEmail() == null) return;

            String body = String.format("""
                Hi %s,

                ⏸️ The queue at %s has been temporarily paused.
                %s

                You'll be notified as soon as the queue resumes.
                Track your position: %s/dashboard/bookings

                — AppointUnified Queue
                """,
                client.getFullName(),
                appointment.getProfessional().getDisplayName(),
                reason != null ? "Reason: " + reason : "",
                frontendUrl);

            send(client.getEmail(), "⏸️ Queue paused — " + appointment.getProfessional().getDisplayName(), body);
        } catch (Exception e) {
            log.error("Pause notification failed: {}", e.getMessage());
        }
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromName + " <" + fromEmail + ">");
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        mailSender.send(msg);
    }
}
