package com.appointunified.service;

import com.appointunified.entity.*;
import com.appointunified.enums.AppointmentStatus;
import com.appointunified.exception.AppException;
import com.appointunified.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * V3 Core: Real-Time Queue Engine
 *
 * State lives in two places:
 *  1. Upstash Redis  — live queue state (fast reads, auto-expires at midnight)
 *  2. Postgres       — source of truth for tokens, events, delay log
 *
 * Redis key format:
 *  queue:{professionalId}            → JSON QueueState object
 *  queue:{professionalId}:paused     → "1" if paused
 *  queue:{professionalId}:delay      → cumulative delay minutes (int string)
 *
 * WebSocket broadcast destinations:
 *  /topic/queue/{professionalId}     → all clients watching this queue
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueueEngineService {

    private final QueueTokenRepository tokenRepository;
    private final QueueEventRepository eventRepository;
    private final DelayLogRepository delayLogRepository;
    private final QueuePauseLogRepository pauseLogRepository;
    private final QueueAnalyticsSnapshotRepository snapshotRepository;
    private final EmergencySlotRegistryRepository emergencyRegistry;
    private final QueueBroadcastRepository broadcastRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate ws;
    private final ObjectMapper objectMapper;
    private final NotificationServiceV3Extension notificationServiceV3Extension;

    private static final String REDIS_KEY = "queue:";
    // ─── Token creation (called when appointment is booked) ─────────────────

    @Transactional
    public QueueTokenView joinQueue(UUID appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> AppException.notFound("Appointment not found"));

        if (tokenRepository.findByAppointmentId(appointmentId).isPresent()) {
            return toView(tokenRepository.findByAppointmentId(appointmentId).get());
        }

        UUID profId = appt.getProfessional().getId();
        int nextToken = tokenRepository.findMaxTokenNumber(profId) + 1;
        long waitingCount = tokenRepository.countWaiting(profId);
        int position = (int) waitingCount + 1;

        // Estimate wait based on average service time (fallback: 30 min per token)
        int estimatedWait = position * getAvgServiceMins(profId);

        QueueToken token = QueueToken.builder()
            .appointment(appt)
            .professional(appt.getProfessional())
            .tokenNumber(nextToken)
            .position(position)
            .estimatedWaitMins(estimatedWait + getCumulativeDelay(profId))
            .status("WAITING")
            .createdAt(OffsetDateTime.now())
            .build();

        token = tokenRepository.save(token);
        appt.setStatus(AppointmentStatus.IN_QUEUE);
        appointmentRepository.save(appt);

        logEvent(appt.getProfessional(), "TOKEN_ISSUED", token.getId(), null,
            Map.of("tokenNumber", nextToken, "position", position));

        broadcastQueueUpdate(profId);
        log.info("Token #{} issued for appointment {} (pos={})", nextToken, appointmentId, position);
        return toView(token);
    }

    // ─── Professional: call next client ─────────────────────────────────────

    @Transactional
    public QueueTokenView callNext(UUID professionalUserId) {
        Professional professional = professionalRepository.findByUserId(professionalUserId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));

        if (isPaused(professional.getId())) {
            throw AppException.badRequest("Queue is paused. Resume before calling next.");
        }

        List<QueueToken> waiting = tokenRepository
            .findByProfessionalIdAndStatusOrderByPosition(professional.getId(), "WAITING");

        if (waiting.isEmpty()) {
            throw AppException.badRequest("No clients waiting in queue.");
        }

        QueueToken next = waiting.get(0);
        next.setStatus("CALLED");
        next.setCalledAt(OffsetDateTime.now());
        tokenRepository.save(next);

        next.getAppointment().setStatus(AppointmentStatus.IN_PROGRESS);
        appointmentRepository.save(next.getAppointment());

        logEvent(professional, "TOKEN_CALLED", next.getId(), professionalUserId,
            Map.of("tokenNumber", next.getTokenNumber()));

        // Notify the client their turn has arrived
        notificationServiceV3Extension.sendQueueCallNotification(next.getAppointment());

        broadcastQueueUpdate(professional.getId());
        log.info("Token #{} called for professional {}", next.getTokenNumber(), professional.getId());
        return toView(next);
    }

    // ─── Professional: trigger delay ────────────────────────────────────────

    @Transactional
    public DelayResult triggerDelay(UUID professionalUserId, int delayMinutes, String reason) {
        Professional professional = professionalRepository.findByUserId(professionalUserId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));

        if (delayMinutes <= 0 || delayMinutes > 240) {
            throw AppException.badRequest("Delay must be between 1 and 240 minutes.");
        }

        List<QueueToken> waitingTokens = tokenRepository
            .findByProfessionalIdAndStatusOrderByPosition(professional.getId(), "WAITING");

        // Propagate delay to all waiting tokens
        waitingTokens.forEach(token -> {
            token.setEstimatedWaitMins((token.getEstimatedWaitMins() != null ? token.getEstimatedWaitMins() : 0) + delayMinutes);
            tokenRepository.save(token);

            Integer updatedEta = token.getEstimatedWaitMins() != null ? token.getEstimatedWaitMins() : 0;
            notificationServiceV3Extension.sendDelayNotification(token.getAppointment(), delayMinutes, updatedEta);
        });

        // Persist delay log
        DelayLog log2 = DelayLog.builder()
            .professional(professional)
            .delayMinutes(delayMinutes)
            .reason(reason)
            .tokensAffected(waitingTokens.size())
            .triggeredBy(professionalUserId)
            .createdAt(OffsetDateTime.now())
            .build();
        delayLogRepository.save(log2);

        // Update Redis cumulative delay
        String delayKey = REDIS_KEY + professional.getId() + ":delay";
        redis.opsForValue().increment(delayKey, delayMinutes);

        logEvent(professional, "DELAY_TRIGGERED", null, professionalUserId,
            Map.of("delayMinutes", delayMinutes, "tokensAffected", waitingTokens.size(), "reason", reason != null ? reason : ""));

        broadcastQueueUpdate(professional.getId());
        log.info("Delay of {}m triggered for professional {} — {} tokens affected",
            delayMinutes, professional.getId(), waitingTokens.size());

        return new DelayResult(delayMinutes, waitingTokens.size(), reason);
    }

    // ─── Professional: pause / resume ───────────────────────────────────────

    @Transactional
    public void pauseQueue(UUID professionalUserId, String reason) {
        Professional professional = professionalRepository.findByUserId(professionalUserId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));

        if (isPaused(professional.getId())) throw AppException.badRequest("Queue is already paused.");

        redis.opsForValue().set(REDIS_KEY + professional.getId() + ":paused", "1");

        List<QueueToken> waitingTokens = tokenRepository
            .findByProfessionalIdAndStatusOrderByPosition(professional.getId(), "WAITING");
        waitingTokens.forEach(token ->
            notificationServiceV3Extension.sendQueuePausedNotification(token.getAppointment(), reason)
        );

        QueuePauseLog pauseLog = QueuePauseLog.builder()
            .professional(professional)
            .reason(reason)
            .pausedAt(OffsetDateTime.now())
            .triggeredBy(professionalUserId)
            .build();
        pauseLogRepository.save(pauseLog);

        logEvent(professional, "QUEUE_PAUSED", null, professionalUserId, Map.of("reason", reason != null ? reason : ""));
        broadcastQueueUpdate(professional.getId());
    }

    @Transactional
    public void resumeQueue(UUID professionalUserId) {
        Professional professional = professionalRepository.findByUserId(professionalUserId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));

        redis.delete(REDIS_KEY + professional.getId() + ":paused");

        // Record resume in pause log
        pauseLogRepository.findFirstByProfessionalIdAndResumedAtIsNullOrderByPausedAtDesc(professional.getId())
            .ifPresent(pl -> {
                pl.setResumedAt(OffsetDateTime.now());
                int durationSecs = (int) Duration.between(pl.getPausedAt(), OffsetDateTime.now()).getSeconds();
                pl.setPauseDurationSecs(durationSecs);
                pauseLogRepository.save(pl);
            });

        logEvent(professional, "QUEUE_RESUMED", null, professionalUserId, null);
        broadcastQueueUpdate(professional.getId());
    }

    // ─── NEW V3 FEATURE 4: Emergency slot insertion ──────────────────────────

    @Transactional
    public QueueTokenView insertEmergency(UUID professionalUserId, UUID appointmentId,
                                          String justification) {
        Professional professional = professionalRepository.findByUserId(professionalUserId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));

        User prof = professional.getUser();
        Appointment appt = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> AppException.notFound("Appointment not found"));

        // Force appointment priority to EMERGENCY
        appt.setPriority(com.appointunified.enums.AppointmentPriority.EMERGENCY);
        appointmentRepository.save(appt);

        // Get current waiting tokens (will be bumped)
        List<QueueToken> waiting = tokenRepository
            .findByProfessionalIdAndStatusOrderByPosition(professional.getId(), "WAITING");
        List<UUID> bumpedIds = waiting.stream().map(QueueToken::getId).toList();

        // Create emergency token at position 1
        int nextToken = tokenRepository.findMaxTokenNumber(professional.getId()) + 1;
        QueueToken emergencyToken = QueueToken.builder()
            .appointment(appt)
            .professional(professional)
            .tokenNumber(nextToken)
            .position(1)
            .estimatedWaitMins(0)
            .status("WAITING")
            .createdAt(OffsetDateTime.now())
            .build();
        tokenRepository.save(emergencyToken);

        // Bump all existing waiting tokens back by 1
        waiting.forEach(t -> { t.setPosition(t.getPosition() + 1); tokenRepository.save(t); });

        // Record in emergency registry
        EmergencySlotRegistry registry = EmergencySlotRegistry.builder()
            .appointment(appt)
            .professional(professional)
            .insertedBy(prof)
            .justification(justification)
            .insertedAt(OffsetDateTime.now())
            .build();
        emergencyRegistry.save(registry);

        logEvent(professional, "EMERGENCY_INSERTED", emergencyToken.getId(), professionalUserId,
            Map.of("justification", justification, "bumpedCount", bumpedIds.size()));

        broadcastQueueUpdate(professional.getId());
        return toView(emergencyToken);
    }

    // ─── NEW V3 FEATURE 5: Broadcast message ────────────────────────────────

    @Transactional
    public void broadcastMessage(UUID professionalUserId, String message, String messageType) {
        Professional professional = professionalRepository.findByUserId(professionalUserId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));

        long waitingCount = tokenRepository.countWaiting(professional.getId());

        QueueBroadcast broadcast = QueueBroadcast.builder()
            .professional(professional)
            .message(message)
            .messageType(messageType != null ? messageType : "INFO")
            .sentToCount((int) waitingCount)
            .sentBy(professional.getUser())
            .createdAt(OffsetDateTime.now())
            .build();
        broadcastRepository.save(broadcast);

        // Send via WebSocket to everyone watching this queue
        ws.convertAndSend("/topic/queue/" + professional.getId(),
            Map.of("type", "BROADCAST", "message", message, "messageType", messageType));

        logEvent(professional, "BROADCAST_SENT", null, professionalUserId,
            Map.of("message", message, "sentTo", waitingCount));
    }

    // ─── Queue status (public) ───────────────────────────────────────────────

    public QueueStatus getStatus(UUID professionalId) {
        List<QueueToken> waiting = tokenRepository
            .findByProfessionalIdAndStatusOrderByPosition(professionalId, "WAITING");
        List<QueueToken> called = tokenRepository
            .findByProfessionalIdAndStatusOrderByPosition(professionalId, "CALLED");

        boolean paused = isPaused(professionalId);
        int cumulativeDelay = getCumulativeDelay(professionalId);
        int avgServiceMins = getAvgServiceMins(professionalId);

        List<QueueTokenView> tokenViews = waiting.stream().map(this::toView).toList();

        Integer currentlyServing = called.isEmpty() ? null : called.get(0).getTokenNumber();

        return new QueueStatus(
            professionalId,
            currentlyServing,
            waiting.size(),
            paused,
            cumulativeDelay,
            avgServiceMins,
            tokenViews
        );
    }

    public QueueStatus getMyStatus(UUID professionalUserId) {
        Professional professional = professionalRepository.findByUserId(professionalUserId)
            .orElseThrow(() -> AppException.notFound("Professional not found"));
        return getStatus(professional.getId());
    }

    public QueueTokenView getMyPosition(UUID appointmentId) {
        QueueToken token = tokenRepository.findByAppointmentId(appointmentId)
            .orElseThrow(() -> AppException.notFound("You are not currently in a queue."));
        return toView(token);
    }

    // ─── NEW V3 FEATURE 1: Analytics snapshot scheduler ─────────────────────

    @Scheduled(cron = "0 */5 * * * *") // every 5 minutes
    @Transactional
    public void captureAnalyticsSnapshot() {
        List<Professional> active = professionalRepository
            .findByVerificationStatus(
                com.appointunified.enums.VerificationStatus.APPROVED,
                org.springframework.data.domain.PageRequest.of(0, 500))
            .getContent();

        active.forEach(p -> {
            try {
                long waiting = tokenRepository.countWaiting(p.getId());
                if (waiting == 0) return; // skip idle queues

                OffsetDateTime todayStart = OffsetDateTime.now().toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
                int cumulativeDelay = delayLogRepository.sumDelaysSince(p.getId(), todayStart);

                QueueAnalyticsSnapshot snap = QueueAnalyticsSnapshot.builder()
                    .professional(p)
                    .snapshotAt(OffsetDateTime.now())
                    .queueLength((int) waiting)
                    .currentWaitMins(getAvgServiceMins(p.getId()) * (int) waiting + cumulativeDelay)
                    .paused(isPaused(p.getId()))
                    .cumulativeDelayMins(cumulativeDelay)
                    .build();
                snapshotRepository.save(snap);
            } catch (Exception e) {
                log.error("Snapshot failed for {}: {}", p.getId(), e.getMessage());
            }
        });
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private boolean isPaused(UUID professionalId) {
        return Boolean.TRUE.equals(redis.hasKey(REDIS_KEY + professionalId + ":paused"));
    }

    private int getCumulativeDelay(UUID professionalId) {
        String val = redis.opsForValue().get(REDIS_KEY + professionalId + ":delay");
        return val != null ? Integer.parseInt(val) : 0;
    }

    private int getAvgServiceMins(UUID professionalId) {
        // Default 30 min; V9 analytics will replace this with real computed average
        return 30;
    }

    private void broadcastQueueUpdate(UUID professionalId) {
        try {
            QueueStatus status = getStatus(professionalId);
            ws.convertAndSend("/topic/queue/" + professionalId,
                Map.of("type", "QUEUE_UPDATE", "data", status));
        } catch (Exception e) {
            log.error("WebSocket broadcast failed: {}", e.getMessage());
        }
    }

    private void logEvent(Professional professional, String eventType, UUID tokenId,
                          UUID triggeredBy, Map<String, Object> payload) {
        QueueEvent event = QueueEvent.builder()
            .professional(professional)
            .eventType(eventType)
            .tokenId(tokenId)
            .triggeredBy(triggeredBy)
            .payload(payload)
            .createdAt(OffsetDateTime.now())
            .build();
        eventRepository.save(event);
    }

    private QueueTokenView toView(QueueToken t) {
        return new QueueTokenView(
            t.getId(), t.getAppointment().getId(),
            t.getTokenNumber(), t.getPosition(),
            t.getEstimatedWaitMins(), t.getStatus(),
            t.getCalledAt(), t.getCreatedAt()
        );
    }

    // ─── Public DTOs ─────────────────────────────────────────────────────────

    public record QueueTokenView(
        UUID tokenId, UUID appointmentId,
        Integer tokenNumber, Integer position,
        Integer estimatedWaitMins, String status,
        OffsetDateTime calledAt, OffsetDateTime createdAt
    ) {}

    public record QueueStatus(
        UUID professionalId, Integer currentlyServing,
        int waitingCount, boolean paused,
        int cumulativeDelayMins, int avgServiceMins,
        List<QueueTokenView> waitingTokens
    ) {}

    public record DelayResult(int delayMinutes, int tokensAffected, String reason) {}
}
