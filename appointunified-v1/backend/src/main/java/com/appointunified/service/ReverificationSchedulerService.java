package com.appointunified.service;

import com.appointunified.entity.Professional;
import com.appointunified.enums.VerificationStatus;
import com.appointunified.repository.ProfessionalRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ReverificationSchedulerService {

    private final ProfessionalRepository professionalRepository;
    private final NotificationServiceV2Extension notificationServiceV2Extension;

    public ReverificationSchedulerService(
            ProfessionalRepository professionalRepository,
            NotificationServiceV2Extension notificationServiceV2Extension) {
        this.professionalRepository = professionalRepository;
        this.notificationServiceV2Extension = notificationServiceV2Extension;
    }

    @Transactional
    @Scheduled(cron = "0 0 9 * * *")
    public void notifyExpiringVerifications() {
        OffsetDateTime threshold = OffsetDateTime.now().plusDays(14);
        List<Professional> expiring = professionalRepository
                .findByVerificationStatusAndVerificationExpiresAtBeforeAndReverificationNotifiedFalse(
                        VerificationStatus.APPROVED,
                        threshold);

        for (Professional professional : expiring) {
            notificationServiceV2Extension.sendReverificationReminder(professional);
            professional.setReverificationNotified(true);
            professionalRepository.save(professional);
        }
    }
}
