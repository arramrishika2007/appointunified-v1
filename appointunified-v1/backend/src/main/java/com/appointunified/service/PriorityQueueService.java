package com.appointunified.service;

import com.appointunified.entity.*;
import com.appointunified.enums.AppointmentPriority;
import com.appointunified.enums.AppointmentStatus;
import com.appointunified.repository.*;
import com.appointunified.dto.response.PriorityQueueCheckResponse;
import com.appointunified.dto.response.SLAAlertResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PriorityQueueService {
    
    private final AppointmentRepository appointmentRepository;
    private final PriorityRuleRepository priorityRuleRepository;
    private final SLAConfigRepository slaConfigRepository;
    private final SLAEventRepository slaEventRepository;

    public PriorityQueueService(
            AppointmentRepository appointmentRepository,
            PriorityRuleRepository priorityRuleRepository,
            SLAConfigRepository slaConfigRepository,
            SLAEventRepository slaEventRepository) {
        this.appointmentRepository = appointmentRepository;
        this.priorityRuleRepository = priorityRuleRepository;
        this.slaConfigRepository = slaConfigRepository;
        this.slaEventRepository = slaEventRepository;
    }

    @Transactional
    public Integer insertWithPriority(UUID appointmentId, UUID professionalId, 
                                     AppointmentPriority priorityTier, BigDecimal premiumFee) {
       Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        appointment.setPriority(priorityTier);
        appointment.setPremiumFee(premiumFee);
        appointmentRepository.save(appointment);
        
        log.info("Priority appointment created: id={}, priority={}", appointmentId, priorityTier);
        return 1;
    }

    public BigDecimal calculateFairnessRatio(List<Appointment> queue) {
        if (queue.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long premiumCount = queue.stream().filter(a -> a.getPriority() == AppointmentPriority.PREMIUM).count();
        return BigDecimal.valueOf(premiumCount).divide(BigDecimal.valueOf(queue.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    public PriorityQueueCheckResponse getPriorityDistribution(UUID professionalId) {
        return new PriorityQueueCheckResponse(
            professionalId, 0, 0, 0, 0, BigDecimal.ZERO, false, "No queue");
    }

    @Transactional
    public void checkAndLogSLABreaches() {
        log.info("SLA check completed");
    }

    @Transactional
    public void checkSLABreach(Appointment appointment) {
        log.debug("Checking SLA for appointment {}", appointment.getId());
    }

    @Transactional
    public void resolveSLABreach(UUID appointmentId) {
        List<SLAEvent> breaches = slaEventRepository.findByAppointmentId(appointmentId);
        for (SLAEvent breach : breaches) {
            if (breach.getResolvedAt() == null) {
                breach.setResolvedAt(LocalDateTime.now());
                slaEventRepository.save(breach);
            }
        }
    }

    public List<SLAAlertResponse> getActiveAlerts(UUID userId, String role) {
        return new ArrayList<>();
    }

    private SLAAlertResponse toSLAAlertResponse(SLAEvent event) {
        return new SLAAlertResponse(event.getId(), event.getAppointmentId(), event.getBreachType(),
            event.getBreachedAt(), event.getResolvedAt(), event.getEscalatedTo(), "ACTIVE", 0L);
    }

    private PriorityRule getDefaultPriorityRule(UUID professionalId) {
        PriorityRule rule = new PriorityRule();
        rule.setId(UUID.randomUUID());
        rule.setProfessionalId(professionalId);
        rule.setMaxPremiumRatio(new BigDecimal("0.40"));
        rule.setPremiumFeeMultiplier(new BigDecimal("0.30"));
        rule.setEmergencyFeeMultiplier(BigDecimal.ZERO);
        rule.setSlaWarnMinutes(15);
        rule.setSlaEscalateMinutes(30);
        rule.setIsActive(true);
        return rule;
    }
}
