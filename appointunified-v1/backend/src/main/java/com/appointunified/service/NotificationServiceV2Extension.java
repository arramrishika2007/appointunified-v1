package com.appointunified.service;

import com.appointunified.entity.Complaint;
import com.appointunified.entity.Professional;
import com.appointunified.repository.ComplaintRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.appointunified.entity.Notification.NotificationType.SYSTEM;
import static com.appointunified.entity.Notification.NotificationType.VERIFICATION;

@Slf4j
@Service
public class NotificationServiceV2Extension {

    private final NotificationService notificationService;
    private final ComplaintRepository complaintRepository;

    public NotificationServiceV2Extension(
            NotificationService notificationService,
            ComplaintRepository complaintRepository) {
        this.notificationService = notificationService;
        this.complaintRepository = complaintRepository;
    }

    public void sendReverificationReminder(Professional professional) {
        if (professional == null || professional.getUser() == null) {
            return;
        }
        notificationService.createNotification(
                professional.getUser().getId(),
                VERIFICATION.name(),
                "Reverification due soon",
                "Your verification will expire soon. Please submit updated documents.",
                "/professional/verification"
        );
        log.info("Reverification reminder queued for professional {}", professional.getId());
    }

    public void sendVerificationApproved(Professional professional) {
        if (professional == null || professional.getUser() == null) {
            return;
        }
        notificationService.createNotification(
                professional.getUser().getId(),
                VERIFICATION.name(),
                "Verification approved",
                "Your professional verification is approved. You can now accept bookings.",
                "/professional/dashboard"
        );
        log.info("Verification approved notification queued for professional {}", professional.getId());
    }

    public void sendComplaintResolvedNotice(UUID complaintId) {
        if (complaintId == null) {
            return;
        }

        Complaint complaint = complaintRepository.findById(complaintId).orElse(null);
        if (complaint == null) {
            log.warn("Cannot create complaint resolved notice. Complaint not found: {}", complaintId);
            return;
        }

        if (complaint.getReporter() != null) {
            notificationService.createNotification(
                    complaint.getReporter().getId(),
                    SYSTEM.name(),
                    "Complaint update",
                    "Your complaint has been reviewed and resolved by admin.",
                    "/notifications"
            );
        }

        if (complaint.getProfessional() != null && complaint.getProfessional().getUser() != null) {
            notificationService.createNotification(
                    complaint.getProfessional().getUser().getId(),
                    SYSTEM.name(),
                    "Complaint status updated",
                    "A complaint associated with your profile has been resolved.",
                    "/notifications"
            );
        }

        log.info("Complaint {} resolution notification queued", complaintId);
    }

    public void sendVerificationRejected(Professional professional, String reason) {
        if (professional == null || professional.getUser() == null) {
            return;
        }
        String details = (reason == null || reason.isBlank()) ? "" : " Reason: " + reason;
        notificationService.createNotification(
                professional.getUser().getId(),
                VERIFICATION.name(),
                "Verification rejected",
                "Your verification was rejected." + details + " Please resubmit required documents.",
                "/professional/verification"
        );
        log.info("Verification rejected notification queued for professional {} with reason: {}", professional.getId(), reason);
    }
}
