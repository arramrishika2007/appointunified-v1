package com.appointunified.service;

import com.appointunified.dto.response.WaitlistResponse;
import com.appointunified.entity.Appointment;
import com.appointunified.entity.Professional;
import com.appointunified.entity.User;
import com.appointunified.entity.WaitlistEntry;
import com.appointunified.exception.AppException;
import com.appointunified.repository.ProfessionalRepository;
import com.appointunified.repository.ServiceRepository;
import com.appointunified.repository.UserRepository;
import com.appointunified.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitlistService {

    private final WaitlistRepository waitlistRepository;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;
    private final ServiceRepository serviceRepository;
    private final NotificationService notificationService;

    @Transactional
    public WaitlistResponse.Summary join(UUID userId,
                                         UUID professionalId,
                                         UUID serviceId,
                                         LocalTime preferredTimeFrom,
                                         LocalTime preferredTimeTo) {
        OffsetDateTime now = OffsetDateTime.now();
        if (waitlistRepository.existsActiveEntry(userId, professionalId, serviceId, now)) {
            throw AppException.conflict("You already have an active waitlist entry for this professional");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        WaitlistEntry entry = new WaitlistEntry();
        entry.setUser(user);
        entry.setProfessional(professional);
        if (serviceId != null) {
            com.appointunified.entity.Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> AppException.notFound("Service not found"));
            if (!service.getProfessional().getId().equals(professionalId)) {
                throw AppException.badRequest("Service does not belong to this professional");
            }
            entry.setService(service);
        }
        entry.setPreferredTimeFrom(preferredTimeFrom);
        entry.setPreferredTimeTo(preferredTimeTo);
        entry.setNotified(false);
        entry.setCreatedAt(now);
        entry.setExpiresAt(now.plusDays(14));

        return toSummary(waitlistRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse.Summary> getMyActive(UUID userId) {
        return waitlistRepository
                .findByUserIdAndNotifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(userId, OffsetDateTime.now())
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancel(UUID userId, UUID entryId) {
        WaitlistEntry entry = waitlistRepository.findById(entryId)
                .orElseThrow(() -> AppException.notFound("Waitlist entry not found"));
        if (!entry.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Not your waitlist entry");
        }
        waitlistRepository.delete(entry);
    }

    @Transactional
    public void notifyNextCandidate(Appointment cancelledAppointment) {
        UUID professionalId = cancelledAppointment.getProfessional().getId();
        UUID serviceId = cancelledAppointment.getService() != null ? cancelledAppointment.getService().getId() : null;

        List<WaitlistEntry> candidates = waitlistRepository
                .findByProfessionalIdAndNotifiedFalseAndExpiresAtAfterOrderByCreatedAtAsc(
                        professionalId,
                        OffsetDateTime.now());

        WaitlistEntry next = candidates.stream()
                .filter(c -> c.getService() == null || (serviceId != null && c.getService().getId().equals(serviceId)))
                .findFirst()
                .orElse(null);

        if (next == null) {
            return;
        }

        next.setNotified(true);
        next.setNotifiedAt(OffsetDateTime.now());
        waitlistRepository.save(next);

        notificationService.sendWaitlistSpotAvailable(next.getUser(), cancelledAppointment);
        log.info("Waitlist candidate {} notified for professional {}", next.getUser().getId(), professionalId);
    }

    private WaitlistResponse.Summary toSummary(WaitlistEntry entry) {
        WaitlistResponse.Summary response = new WaitlistResponse.Summary();
        response.setId(entry.getId());
        response.setProfessionalId(entry.getProfessional().getId());
        response.setProfessionalName(entry.getProfessional().getDisplayName());
        response.setServiceId(entry.getService() != null ? entry.getService().getId() : null);
        response.setServiceName(entry.getService() != null ? entry.getService().getName() : null);
        response.setNotified(entry.isNotified());
        response.setNotifiedAt(entry.getNotifiedAt());
        response.setPreferredTimeFrom(entry.getPreferredTimeFrom());
        response.setPreferredTimeTo(entry.getPreferredTimeTo());
        response.setCreatedAt(entry.getCreatedAt());
        response.setExpiresAt(entry.getExpiresAt());
        return response;
    }
}
