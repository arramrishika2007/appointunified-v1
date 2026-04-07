package com.appointunified.service;

import com.appointunified.dto.request.AppointmentRequest;
import com.appointunified.dto.response.AppointmentResponse;
import com.appointunified.entity.*;
import com.appointunified.enums.AppointmentPriority;
import com.appointunified.enums.AppointmentStatus;
import com.appointunified.exception.AppException;
import com.appointunified.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final BookingDraftRepository bookingDraftRepository;
    private final NotificationService notificationService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    @CacheEvict(cacheNames = "slotRecommendations", allEntries = true)
    public AppointmentResponse.Summary create(UUID clientId, AppointmentRequest.Create request) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        Professional professional = professionalRepository.findById(request.getProfessionalId())
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        if (!professional.isAcceptingBookings()) {
            throw AppException.badRequest("This professional is not currently accepting bookings");
        }

        com.appointunified.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> AppException.notFound("Service not found"));

        if (!service.getProfessional().getId().equals(professional.getId())) {
            throw AppException.badRequest("Service does not belong to this professional");
        }

        OffsetDateTime startTime = request.getStartTime();
        OffsetDateTime endTime = startTime.plusMinutes(service.getDurationMinutes());

        // Conflict detection
        if (appointmentRepository.hasConflict(professional.getId(), startTime, endTime)) {
            throw AppException.conflict("This time slot is no longer available. Please select another.");
        }

        AppointmentPriority priority = AppointmentPriority.NORMAL;
        if (request.getPriority() != null) {
            priority = AppointmentPriority.valueOf(request.getPriority().toUpperCase());
        }

        // Generate share token (NEW V1 FEATURE 4)
        String shareToken = generateShareToken();

        Appointment appointment = new Appointment();
        appointment.setClient(client);
        appointment.setProfessional(professional);
        appointment.setService(service);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setPriority(priority);
        appointment.setNotes(request.getNotes());
        appointment.setVirtual(request.isVirtual());
        appointment.setShareToken(shareToken);
        appointment.setShareExpiresAt(OffsetDateTime.now().plusDays(30));

        appointment = appointmentRepository.save(appointment);
        log.info("Appointment created: {} for client {} with professional {}",
                appointment.getId(), clientId, professional.getId());

        // Send confirmation notifications async
        notificationService.sendBookingConfirmation(appointment.getId());

        return toSummaryResponse(appointment);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse.Summary> getMyAppointments(UUID clientId, Pageable pageable) {
        return appointmentRepository.findByClientIdOrderByStartTimeDesc(clientId, pageable)
                .map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse.Summary> getProfessionalAppointments(UUID professionalId, Pageable pageable) {
        return appointmentRepository.findByProfessionalIdOrderByStartTimeAsc(professionalId, pageable)
                .map(this::toSummaryResponse);
    }

    @Transactional
    @CacheEvict(cacheNames = "slotRecommendations", allEntries = true)
    public AppointmentResponse.Summary cancel(UUID appointmentId, UUID requesterId,
                                               AppointmentRequest.Cancel request) {
        Appointment appointment = findAndValidate(appointmentId, requesterId);

        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
                appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw AppException.badRequest("Cannot cancel a " + appointment.getStatus().name().toLowerCase() + " appointment");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.getReason());
        appointment.setCancelledBy(requester);
        appointment.setCancelledAt(OffsetDateTime.now());

        appointment = appointmentRepository.save(appointment);
        notificationService.sendCancellationNotification(appointment.getId());

        return toSummaryResponse(appointment);
    }

    @Transactional
    @CacheEvict(cacheNames = "slotRecommendations", allEntries = true)
    public AppointmentResponse.Summary reschedule(UUID appointmentId, UUID requesterId,
                                                   AppointmentRequest.Reschedule request) {
        Appointment appointment = findAndValidate(appointmentId, requesterId);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw AppException.badRequest("Only scheduled appointments can be rescheduled");
        }

        OffsetDateTime newStart = request.getNewStartTime();
        OffsetDateTime newEnd = newStart.plusMinutes(appointment.getService().getDurationMinutes());

        if (appointmentRepository.hasConflict(appointment.getProfessional().getId(), newStart, newEnd)) {
            throw AppException.conflict("The requested time slot is not available");
        }

        appointment.setStartTime(newStart);
        appointment.setEndTime(newEnd);
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        appointment = appointmentRepository.save(appointment);
        notificationService.sendRescheduleNotification(appointment.getId());

        return toSummaryResponse(appointment);
    }

    @Transactional
    @CacheEvict(cacheNames = "slotRecommendations", allEntries = true)
    public AppointmentResponse.Summary markComplete(UUID appointmentId, UUID professionalUserId) {
        Professional professional = professionalRepository.findByUserId(professionalUserId)
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.notFound("Appointment not found"));

        if (!appointment.getProfessional().getId().equals(professional.getId())) {
            throw AppException.forbidden("Not your appointment");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(OffsetDateTime.now());
        professional.setTotalCompleted(professional.getTotalCompleted() + 1);
        professionalRepository.save(professional);

        return toSummaryResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    @CacheEvict(cacheNames = "slotRecommendations", allEntries = true)
    public AppointmentResponse.Summary markNoShow(UUID appointmentId, UUID professionalUserId) {
        Professional professional = professionalRepository.findByUserId(professionalUserId)
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.notFound("Appointment not found"));

        if (!appointment.getProfessional().getId().equals(professional.getId())) {
            throw AppException.forbidden("Not your appointment");
        }

        appointment.setStatus(AppointmentStatus.NO_SHOW);
        return toSummaryResponse(appointmentRepository.save(appointment));
    }

    // NEW V1 FEATURE 4: Get share info for appointment
    @Transactional(readOnly = true)
    public AppointmentResponse.ShareInfo getShareInfo(UUID appointmentId, UUID requesterId) {
        Appointment appointment = findAndValidate(appointmentId, requesterId);

        String shareUrl = frontendUrl + "/appointments/share/" + appointment.getShareToken();
        String icalUrl = frontendUrl + "/api/appointments/" + appointmentId + "/ical";

        AppointmentResponse.ShareInfo shareInfo = new AppointmentResponse.ShareInfo();
        shareInfo.setShareUrl(shareUrl);
        shareInfo.setIcalUrl(icalUrl);
        shareInfo.setExpiresAt(appointment.getShareExpiresAt());
        return shareInfo;
    }

    // Public shared appointment view (no auth needed)
    @Transactional(readOnly = true)
    public AppointmentResponse.Summary getByShareToken(String shareToken) {
        Appointment appointment = appointmentRepository.findByShareToken(shareToken)
                .orElseThrow(() -> AppException.notFound("Appointment not found or link expired"));

        if (appointment.getShareExpiresAt().isBefore(OffsetDateTime.now())) {
            throw AppException.badRequest("This appointment link has expired");
        }

        return toSummaryResponse(appointment);
    }

    // NEW V1 FEATURE 3: Save booking draft
    @Transactional
    public AppointmentResponse.DraftSummary saveDraft(UUID userId, AppointmentRequest.SaveDraft request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        // Find existing draft for same professional or create new
        List<BookingDraft> existing = bookingDraftRepository
                .findByUserIdAndExpiresAtAfterOrderByUpdatedAtDesc(userId, OffsetDateTime.now());

        BookingDraft draft;
        if (!existing.isEmpty() && existing.get(0).getProfessional() != null &&
                existing.get(0).getProfessional().getId().equals(request.getProfessionalId())) {
            draft = existing.get(0); // Update existing
        } else {
            draft = new BookingDraft();
            draft.setUser(user);
            draft.setExpiresAt(OffsetDateTime.now().plusHours(48));
        }

        if (request.getProfessionalId() != null) {
            professionalRepository.findById(request.getProfessionalId())
                    .ifPresent(draft::setProfessional);
        }
        if (request.getServiceId() != null) {
            serviceRepository.findById(request.getServiceId())
                    .ifPresent(draft::setService);
        }

        draft.setStepReached(request.getStepReached() != null ? request.getStepReached() : 1);
        draft.setDraftData(request.getDraftData() != null ? request.getDraftData() : new HashMap<>());
        draft.setUpdatedAt(OffsetDateTime.now());

        draft = bookingDraftRepository.save(draft);
        return toDraftResponse(draft);
    }

    public List<AppointmentResponse.DraftSummary> getMyDrafts(UUID userId) {
        return bookingDraftRepository
                .findByUserIdAndExpiresAtAfterOrderByUpdatedAtDesc(userId, OffsetDateTime.now())
                .stream()
                .map(this::toDraftResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDraft(UUID draftId, UUID userId) {
        BookingDraft draft = bookingDraftRepository.findById(draftId)
                .orElseThrow(() -> AppException.notFound("Draft not found"));
        if (!draft.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Not your draft");
        }
        bookingDraftRepository.delete(draft);
    }

    // Cleanup expired drafts daily
    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    @Transactional
    public void cleanupExpiredDrafts() {
        int deleted = bookingDraftRepository.deleteExpiredDrafts(OffsetDateTime.now());
        log.info("Cleaned up {} expired booking drafts", deleted);
    }

    // ─── iCal generation (NEW V1 FEATURE 4) ────────────────────────────────

    @Transactional(readOnly = true)
    public String generateIcal(UUID appointmentId) {
        Appointment a = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.notFound("Appointment not found"));

        String uid = a.getId().toString() + "@appointunified.com";
        String dtStart = formatIcalDate(a.getStartTime());
        String dtEnd = formatIcalDate(a.getEndTime());
        String summary = a.getService().getName() + " with " + a.getProfessional().getDisplayName();
        String location = a.isVirtual() ? "Virtual (Online)" :
                (a.getProfessional().getAddress() != null ? a.getProfessional().getAddress() : "");

        return "BEGIN:VCALENDAR\r\n" +
               "VERSION:2.0\r\n" +
               "PRODID:-//AppointUnified//EN\r\n" +
               "BEGIN:VEVENT\r\n" +
               "UID:" + uid + "\r\n" +
               "DTSTAMP:" + formatIcalDate(OffsetDateTime.now()) + "\r\n" +
               "DTSTART:" + dtStart + "\r\n" +
               "DTEND:" + dtEnd + "\r\n" +
               "SUMMARY:" + summary + "\r\n" +
               "LOCATION:" + location + "\r\n" +
               "DESCRIPTION:Booked via AppointUnified. Appointment ID: " + a.getId() + "\r\n" +
               "STATUS:CONFIRMED\r\n" +
               "END:VEVENT\r\n" +
               "END:VCALENDAR\r\n";
    }

    private String formatIcalDate(OffsetDateTime dt) {
        return dt.toInstant().toString().replace("-", "").replace(":", "").replace(".000Z", "Z");
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Appointment findAndValidate(UUID appointmentId, UUID requesterId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.notFound("Appointment not found"));

        boolean isClient = appointment.getClient().getId().equals(requesterId);
        boolean isProfessionalUser = appointment.getProfessional().getUser().getId().equals(requesterId);

        if (!isClient && !isProfessionalUser) {
            throw AppException.forbidden("You do not have access to this appointment");
        }
        return appointment;
    }

    private String generateShareToken() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private AppointmentResponse.Summary toSummaryResponse(Appointment a) {
        AppointmentResponse.ProfessionalInfo professionalInfo = new AppointmentResponse.ProfessionalInfo();
        professionalInfo.setId(a.getProfessional().getId());
        professionalInfo.setDisplayName(a.getProfessional().getDisplayName());
        professionalInfo.setAvatarUrl(a.getProfessional().getAvatarUrl());
        professionalInfo.setSector(a.getProfessional().getSector().name());
        professionalInfo.setSpecialty(a.getProfessional().getSpecialty());

        AppointmentResponse.ServiceInfo serviceInfo = new AppointmentResponse.ServiceInfo();
        serviceInfo.setId(a.getService().getId());
        serviceInfo.setName(a.getService().getName());
        serviceInfo.setDurationMinutes(a.getService().getDurationMinutes());
        serviceInfo.setPrice(a.getService().getPrice());

        AppointmentResponse.ClientInfo clientInfo = new AppointmentResponse.ClientInfo();
        clientInfo.setId(a.getClient().getId());
        clientInfo.setFullName(a.getClient().getFullName());
        clientInfo.setPhone(a.getClient().getPhone());
        clientInfo.setAvatarUrl(a.getClient().getAvatarUrl());

        AppointmentResponse.Summary summary = new AppointmentResponse.Summary();
        summary.setId(a.getId());
        summary.setStatus(a.getStatus().name());
        summary.setPriority(a.getPriority().name());
        summary.setStartTime(a.getStartTime());
        summary.setEndTime(a.getEndTime());
        summary.setNotes(a.getClientNotes());
        summary.setVirtual(a.isVirtual());
        summary.setMeetLink(a.getMeetLink());
        summary.setShareToken(a.getShareToken());
        summary.setIcalUrl("/appointments/" + a.getId() + "/ical");
        summary.setCreatedAt(a.getCreatedAt());
        summary.setProfessional(professionalInfo);
        summary.setService(serviceInfo);
        summary.setClient(clientInfo);
        return summary;
    }

    private AppointmentResponse.DraftSummary toDraftResponse(BookingDraft d) {
        AppointmentResponse.DraftSummary draftSummary = new AppointmentResponse.DraftSummary();
        draftSummary.setId(d.getId());
        draftSummary.setProfessionalId(d.getProfessional() != null ? d.getProfessional().getId() : null);
        draftSummary.setProfessionalName(d.getProfessional() != null ? d.getProfessional().getDisplayName() : null);
        draftSummary.setServiceId(d.getService() != null ? d.getService().getId() : null);
        draftSummary.setServiceName(d.getService() != null ? d.getService().getName() : null);
        draftSummary.setStepReached(d.getStepReached());
        draftSummary.setDraftData(d.getDraftData());
        draftSummary.setExpiresAt(d.getExpiresAt());
        draftSummary.setUpdatedAt(d.getUpdatedAt());
        return draftSummary;
    }
}
