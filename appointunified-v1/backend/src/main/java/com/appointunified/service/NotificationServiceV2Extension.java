package com.appointunified.service;

import com.appointunified.entity.Professional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationServiceV2Extension {

    public void sendReverificationReminder(Professional professional) {
        log.info("Reverification reminder queued for professional {}", professional.getId());
    }

    public void sendVerificationApproved(Professional professional) {
        log.info("Verification approved notification queued for professional {}", professional.getId());
    }

    public void sendComplaintResolvedNotice(String complaintId) {
        log.info("Complaint {} resolution notification queued", complaintId);
    }

    public void sendVerificationRejected(Professional professional, String reason) {
        log.info("Verification rejected notification queued for professional {} with reason: {}", professional.getId(), reason);
    }
}
