package com.appointunified.service;

import com.appointunified.entity.AuditLog;
import com.appointunified.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central audit logging service.
 * All writes are async and non-blocking so they never impact
 * the main request/response cycle.
 *
 * Usage:
 *   auditService.log(userId, "APPROVE_PROFESSIONAL",
 *       "professional", professionalId, Map.of("reason", "docs verified"), ip);
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async("notificationExecutor")
    public void log(UUID actorId,
                    String action,
                    String targetType,
                    UUID targetId,
                    Map<String, Object> metadata,
                    String ipAddress) {
        try {
            AuditLog entry = new AuditLog();
            entry.setActorId(actorId);
            entry.setAction(action);
            entry.setTargetType(targetType);
            entry.setTargetId(targetId);
            entry.setMetadata(metadata);
            entry.setIpAddress(ipAddress);
            entry.setCreatedAt(OffsetDateTime.now());
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Never let audit failure propagate
            log.error("Failed to write audit log for action {}: {}", action, e.getMessage());
        }
    }

    /** Convenience overload without metadata */
    @Async("notificationExecutor")
    public void log(UUID actorId, String action, String targetType, UUID targetId) {
        log(actorId, action, targetType, targetId, null, null);
    }

    /** System action (no actor) */
    @Async("notificationExecutor")
    public void logSystem(String action, String targetType, UUID targetId, Map<String, Object> metadata) {
        log(null, action, targetType, targetId, metadata, "system");
    }
}
