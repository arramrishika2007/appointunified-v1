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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final GroqService groqService;
    private final BehaviorScoringService behaviorScoringService;
    private final WaitlistService waitlistService;
    private final WorkflowEngineService workflowEngineService;
    private final PriorityQueueService priorityQueueService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.meeting.strict-window:false}")
    private boolean strictMeetingJoinWindow;

    @Transactional
    @CacheEvict(cacheNames = "slotRecommendations", allEntries = true)
    public AppointmentResponse.Summary create(UUID clientId, AppointmentRequest.Create request) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (client.isBlockedForUnpaid()) {
            throw AppException.forbidden("Your account is locked due to unpaid balances. Please clear them to book again.");
        }

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
        appointment.setTotalAmount(service.getPrice());

        // Offline-first policy: in-person bookings do not require payment confirmation gates.
        if (!request.isVirtual()) {
            appointment.setDepositStatus(com.appointunified.enums.PaymentStatus.CONFIRMED);
            appointment.setFinalPaymentStatus(com.appointunified.enums.PaymentStatus.CONFIRMED);
        }

        if (request.isVirtual()) {
            // Generate a 6-digit meeting token and keep join URL token-based.
            String token = String.format("%06d", new SecureRandom().nextInt(999999));
            appointment.setMeetingToken(token);
            appointment.setMeetLink(frontendUrl + "/meeting/" + token);
        } else {
            appointment.setClientLat(request.getClientLat());
            appointment.setClientLon(request.getClientLon());
            // Basic Haversine distance if lat/lons exist
            if (request.getClientLat() != null && request.getClientLon() != null &&
                professional.getLatitude() != null && professional.getLongitude() != null) {
                double dist = calculateDistanceMeters(request.getClientLat(), request.getClientLon(),
                        professional.getLatitude().doubleValue(), professional.getLongitude().doubleValue());
                appointment.setDistanceMeters((int) dist);
            }
        }

        appointment.setShareToken(shareToken);
        appointment.setShareExpiresAt(OffsetDateTime.now().plusDays(30));
        if (request.getWorkflowInstanceId() != null) {
            appointment.setWorkflowInstanceId(request.getWorkflowInstanceId());
        }

        appointment = appointmentRepository.save(appointment);

        if (request.getWorkflowInstanceId() != null) {
            workflowEngineService.linkAppointmentToWorkflowStep(
                    clientId,
                    request.getWorkflowInstanceId(),
                    request.getWorkflowStepOrder(),
                    appointment
            );
        }

        log.info("Appointment created: {} for client {} with professional {}",
                appointment.getId(), clientId, professional.getId());

        // Send confirmation notifications only after the DB commit is successful.
        final UUID createdAppointmentId = appointment.getId();
        runAfterCommit(() -> notificationService.sendBookingConfirmation(createdAppointmentId));

        return toSummaryResponse(appointment);
    }

    @Transactional
    public AppointmentResponse.Summary lockSlot(UUID clientId, AppointmentRequest.Create request) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (client.isBlockedForUnpaid()) {
            throw AppException.forbidden("Your account is locked due to unpaid balances. Please clear them to book again.");
        }

        Professional professional = professionalRepository.findById(request.getProfessionalId())
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        if (!professional.isAcceptingBookings()) {
            throw AppException.badRequest("This professional is not currently accepting bookings");
        }

        com.appointunified.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> AppException.notFound("Service not found"));

        OffsetDateTime startTime = request.getStartTime();
        OffsetDateTime endTime = startTime.plusMinutes(service.getDurationMinutes());

        if (appointmentRepository.hasConflict(professional.getId(), startTime, endTime)) {
            throw AppException.conflict("This time slot is no longer available. Please select another.");
        }

        Appointment appointment = new Appointment();
        appointment.setClient(client);
        appointment.setProfessional(professional);
        appointment.setService(service);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        // Lock slot specifically uses PENDING_DEPOSIT
        appointment.setStatus(AppointmentStatus.PENDING_DEPOSIT);
        appointment.setDepositStatus(com.appointunified.enums.PaymentStatus.PENDING);
        appointment.setFinalPaymentStatus(com.appointunified.enums.PaymentStatus.PENDING);
        
        appointment.setPriority(request.getPriority() != null ? AppointmentPriority.valueOf(request.getPriority().toUpperCase()) : AppointmentPriority.NORMAL);
        appointment.setNotes(request.getNotes());
        appointment.setVirtual(request.isVirtual());
        appointment.setTotalAmount(service.getPrice());

        appointment = appointmentRepository.save(appointment);
        log.info("Slot locked for appointment: {}", appointment.getId());
        return toSummaryResponse(appointment);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse.Summary> getMyAppointments(UUID clientId, Pageable pageable) {
        return appointmentRepository.findByClientIdOrderByStartTimeDesc(clientId, pageable)
                .map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse.Summary getMyAppointmentById(UUID appointmentId, UUID requesterId) {
        Appointment appointment = findAndValidate(appointmentId, requesterId);
        return toSummaryResponse(appointment);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse.Summary> getProfessionalAppointments(UUID professionalId, Pageable pageable) {
        return appointmentRepository.findByProfessionalIdOrderByStartTimeAsc(professionalId, pageable)
                .map(this::toSummaryResponse);
    }

        @Transactional(readOnly = true)
        public Page<AppointmentResponse.Summary> getMyProfessionalAppointments(UUID professionalUserId, Pageable pageable) {
        Professional professional = professionalRepository.findByUserId(professionalUserId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));
        return appointmentRepository.findByProfessionalIdOrderByStartTimeAsc(professional.getId(), pageable)
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
        final UUID cancelledAppointmentId = appointment.getId();
        runAfterCommit(() -> notificationService.sendCancellationNotification(cancelledAppointmentId));
        behaviorScoringService.registerCancellation(appointment, request.getReason());
        waitlistService.notifyNextCandidate(appointment);

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
        final UUID rescheduledAppointmentId = appointment.getId();
        runAfterCommit(() -> notificationService.sendRescheduleNotification(rescheduledAppointmentId));

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

        appointment.setCompletedAt(OffsetDateTime.now());
        
        // Split payment logic: Require 70% post-meeting if virtual and not already confirmed
        if (appointment.isVirtual() && appointment.getFinalPaymentStatus() != com.appointunified.enums.PaymentStatus.CONFIRMED) {
             appointment.setStatus(AppointmentStatus.PENDING_BALANCE);
        } else {
             appointment.setStatus(AppointmentStatus.COMPLETED);
        }
        
        professional.setTotalCompleted(professional.getTotalCompleted() + 1);
        professionalRepository.save(professional);
        behaviorScoringService.registerCompletion(appointment);
        workflowEngineService.handleAppointmentCompleted(appointment);
        priorityQueueService.resolveSLABreach(appointment.getId());

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
        behaviorScoringService.registerNoShow(appointment);
        return toSummaryResponse(appointmentRepository.save(appointment));
    }

    // NEW V1 FEATURE 5: Confirm Deposit Payment
    @Transactional
    @CacheEvict(cacheNames = "slotRecommendations", allEntries = true)
    public AppointmentResponse.Summary confirmDepositAndBook(UUID appointmentId, UUID clientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.notFound("Appointment not found"));

        if (!appointment.getClient().getId().equals(clientId)) {
            throw AppException.forbidden("Not your appointment");
        }

        // MVP payment confirmation: move lock-state bookings to schedulable state.
        if (appointment.getDepositStatus() != com.appointunified.enums.PaymentStatus.CONFIRMED) {
             appointment.setDepositStatus(com.appointunified.enums.PaymentStatus.CONFIRMED);
        }

        if (appointment.getStatus() == AppointmentStatus.PENDING_DEPOSIT ||
                appointment.getStatus() == AppointmentStatus.DEPOSIT_PAID ||
                appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            appointment.setStatus(AppointmentStatus.SCHEDULED);
        }

        appointment = appointmentRepository.save(appointment);
        return toSummaryResponse(appointment);
    }

    // NEW V1 FEATURE 5: Verify Final Payment (Professional action)
    @Transactional
    public AppointmentResponse.Summary verifyFinalPayment(UUID appointmentId, UUID professionalUserId) {
        Professional professional = professionalRepository.findByUserId(professionalUserId)
                .orElseThrow(() -> AppException.notFound("Professional not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> AppException.notFound("Appointment not found"));

        if (!appointment.getProfessional().getId().equals(professional.getId())) {
            throw AppException.forbidden("Not your appointment");
        }

        appointment.setFinalPaymentStatus(com.appointunified.enums.PaymentStatus.CONFIRMED);
        
        // Unblock user if they were blocked due to this payment
        if (appointment.getClient().isBlockedForUnpaid()) {
            User client = appointment.getClient();
            client.setBlockedForUnpaid(false); // simplification: unblocks them entirely for now
            client.setUnpaidBalance(java.math.BigDecimal.ZERO);
            userRepository.save(client);
        }

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

    @Transactional(readOnly = true)
    public AppointmentResponse.MeetingJoinInfo validateMeetingToken(String meetingToken) {
        String normalized = meetingToken == null ? "" : meetingToken.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z0-9]{6,12}$")) {
            throw AppException.badRequest("Invalid meeting token format");
        }

        Appointment appointment = appointmentRepository.findFirstByMeetingTokenIgnoreCase(normalized)
                .orElseThrow(() -> AppException.notFound("Meeting token not found"));

        if (!appointment.isVirtual()) {
            throw AppException.badRequest("This appointment is not a virtual meeting");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime joinStartsAt = appointment.getStartTime().minusMinutes(15);
        OffsetDateTime joinEndsAt = appointment.getEndTime().plusMinutes(30);

        AppointmentResponse.MeetingJoinInfo info = new AppointmentResponse.MeetingJoinInfo();
        info.setAppointmentId(appointment.getId());
        info.setMeetingToken(normalized);
        info.setJoinUrl(frontendUrl + "/meeting/" + normalized);
        info.setStatus(appointment.getStatus().name());
        info.setProfessionalName(appointment.getProfessional().getDisplayName());
        info.setServiceName(appointment.getService().getName());
        info.setStartTime(appointment.getStartTime());
        info.setEndTime(appointment.getEndTime());

        boolean inJoinWindow = !now.isBefore(joinStartsAt) && !now.isAfter(joinEndsAt);

        boolean statusJoinable = appointment.getStatus() == AppointmentStatus.SCHEDULED ||
                appointment.getStatus() == AppointmentStatus.CONFIRMED ||
                appointment.getStatus() == AppointmentStatus.DEPOSIT_PAID ||
                appointment.getStatus() == AppointmentStatus.IN_MEETING ||
                appointment.getStatus() == AppointmentStatus.IN_PROGRESS ||
                appointment.getStatus() == AppointmentStatus.PENDING_BALANCE ||
                appointment.getStatus() == AppointmentStatus.PAID_FULL ||
                appointment.getStatus() == AppointmentStatus.COMPLETED;

        if (!statusJoinable) {
            info.setCanJoin(false);
            info.setReason("Meeting is not available because appointment status is " + appointment.getStatus().name());
            return info;
        }

        if (strictMeetingJoinWindow && !inJoinWindow) {
            info.setCanJoin(false);
            if (now.isBefore(joinStartsAt)) {
                info.setReason("Meeting has not started yet");
            } else {
                info.setReason("Meeting join window has ended");
            }
            return info;
        }

        info.setCanJoin(true);
        info.setReason(strictMeetingJoinWindow ? "Meeting is ready to join" : "Meeting is ready to join (demo mode window relaxed)");
        return info;
    }

    @Transactional(readOnly = true)
    public AppointmentResponse.BookingForm generateBookingForm(UUID appointmentId, UUID requesterId) {
        Appointment appointment = findAndValidate(appointmentId, requesterId);

        String bookingToken = appointment.getMeetingToken();
        if (bookingToken == null || bookingToken.isBlank()) {
            String share = appointment.getShareToken();
            bookingToken = (share != null && share.length() >= 8)
                    ? share.substring(0, 8).toUpperCase(Locale.ROOT)
                    : appointment.getId().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        }

        double distanceKm = appointment.getDistanceMeters() != null
                ? Math.round((appointment.getDistanceMeters() / 1000.0) * 100.0) / 100.0
                : 0.0;

        String mode = appointment.isVirtual() ? "ONLINE" : "OFFLINE";
        String clientName = appointment.getClient() != null && appointment.getClient().getFullName() != null
            ? appointment.getClient().getFullName()
            : "Client";
        String professionalName = appointment.getProfessional() != null && appointment.getProfessional().getDisplayName() != null
            ? appointment.getProfessional().getDisplayName()
            : "Professional";
        String serviceName = appointment.getService() != null && appointment.getService().getName() != null
            ? appointment.getService().getName()
            : "Service";

        String prompt = String.format("""
                Generate a concise booking receipt in plain text for a user.
                Keep it under 16 lines, no markdown.
                Must include this exact closing line: Happy consultancy.

                Appointment ID: %s
                Booking Token: %s
                Client Name: %s
                Professional: %s
                Service: %s
                Mode: %s
                Start Time: %s
                End Time: %s
                Distance (km): %.2f
                Total Amount: %s
                """,
                appointment.getId(),
                bookingToken,
                clientName,
                professionalName,
                serviceName,
                mode,
                appointment.getStartTime(),
                appointment.getEndTime(),
                distanceKm,
                appointment.getTotalAmount()
        );

        String content = "";
        try {
            content = groqService.generateCompletion(prompt, 700, 0.3f);
        } catch (Exception ex) {
            log.warn("Booking form AI generation failed for appointment {}. Falling back to template.", appointmentId, ex);
        }

        if (content == null || content.isBlank()) {
            content = String.format("""
                    Booking Receipt
                    Appointment ID: %s
                    Booking Token: %s
                    Client: %s
                    Professional: %s
                    Service: %s
                    Mode: %s
                    Start: %s
                    End: %s
                    Distance: %.2f km
                    Total: %s
                    Happy consultancy.
                    """,
                    appointment.getId(),
                    bookingToken,
                    clientName,
                    professionalName,
                    serviceName,
                    mode,
                    appointment.getStartTime(),
                    appointment.getEndTime(),
                    distanceKm,
                    appointment.getTotalAmount()
            );
        }

        AppointmentResponse.BookingForm form = new AppointmentResponse.BookingForm();
        form.setAppointmentId(appointment.getId());
        form.setBookingToken(bookingToken);
        form.setDistanceKm(distanceKm);
        form.setGeneratedAt(OffsetDateTime.now());
        form.setContent(content.trim());
        return form;
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

        Short stepReached = request.getStepReached();
        if (stepReached == null) {
            stepReached = Short.valueOf((short) 1);
        }
        draft.setStepReached(stepReached);
        draft.setDraftData(request.getDraftData() != null ? request.getDraftData() : new HashMap<>());
        draft.setUpdatedAt(OffsetDateTime.now());

        draft = bookingDraftRepository.save(draft);
        return toDraftResponse(draft);
    }

    @Transactional(readOnly = true)
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

    // Free locked slots that were unpaid > 5 mins
    @Scheduled(fixedRate = 60000) // 1 minute
    @Transactional
    public void cleanupExpiredSlotLocks() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(5);
        List<Appointment> expiredLocks = appointmentRepository.findByStatusAndCreatedAtBefore(AppointmentStatus.PENDING_DEPOSIT, threshold);
        for (Appointment a : expiredLocks) {
            a.setStatus(AppointmentStatus.EXPIRED);
            appointmentRepository.save(a);
            log.info("Cleared expired 5-minute locked slot for appointment {}", a.getId());
        }
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

        summary.setMeetingToken(a.getMeetingToken());
        summary.setClientLat(a.getClientLat());
        summary.setClientLon(a.getClientLon());
        summary.setDistanceMeters(a.getDistanceMeters());
        summary.setDepositStatus(a.getDepositStatus() != null ? a.getDepositStatus().name() : null);
        summary.setFinalPaymentStatus(a.getFinalPaymentStatus() != null ? a.getFinalPaymentStatus().name() : null);
        summary.setTotalAmount(a.getTotalAmount());

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

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radius of the earth in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }
}
