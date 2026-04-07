package com.appointunified.controller;

import com.appointunified.dto.request.PriorityBookingRequest;
import com.appointunified.dto.response.AppointmentResponse;
import com.appointunified.dto.response.PriorityQueueCheckResponse;
import com.appointunified.dto.response.SLAAlertResponse;
import com.appointunified.entity.Appointment;
import com.appointunified.entity.Professional;
import com.appointunified.entity.Service;
import com.appointunified.entity.User;
import com.appointunified.enums.AppointmentPriority;
import com.appointunified.enums.AppointmentStatus;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.ProfessionalRepository;
import com.appointunified.repository.ServiceRepository;
import com.appointunified.repository.UserRepository;
import com.appointunified.service.AppointmentService;
import com.appointunified.service.PriorityQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/priority")
@Slf4j
public class PriorityController {
    
    private final PriorityQueueService priorityQueueService;
    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final UserRepository userRepository;

    public PriorityController(
            PriorityQueueService priorityQueueService,
            AppointmentService appointmentService,
            AppointmentRepository appointmentRepository,
            ServiceRepository serviceRepository,
            ProfessionalRepository professionalRepository,
            UserRepository userRepository) {
        this.priorityQueueService = priorityQueueService;
        this.appointmentService = appointmentService;
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
        this.professionalRepository = professionalRepository;
        this.userRepository = userRepository;
    }

    /**
     * Book an appointment with priority tier (PREMIUM or EMERGENCY).
     * Calculates premium fee based on service price and priority tier.
     * Trigger queue reorder automatically.
     */
    @PostMapping("/book")
    public ResponseEntity<?> bookWithPriority(
            @RequestBody PriorityBookingRequest request,
            Authentication auth) {
        
        try {
            if (auth == null || auth.getPrincipal() == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            
            UUID userId = UUID.fromString((String) auth.getPrincipal());
            
            // Validate request
            if (request.getPriorityTier() == null || request.getServiceId() == null) {
                return ResponseEntity.badRequest()
                    .body("Missing priorityTier or serviceId");
            }
            
            // Load required entities
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            com.appointunified.entity.Service service = serviceRepository
                    .findById(request.getServiceId())
                    .orElseThrow(() -> new IllegalArgumentException("Service not found"));
            
            Professional professional = professionalRepository
                    .findById(request.getProfessionalId())
                    .orElseThrow(() -> new IllegalArgumentException("Professional not found"));
            
            AppointmentPriority priority = AppointmentPriority.valueOf(request.getPriorityTier());
            
            // Calculate premium fee
            BigDecimal premiumFee = calculatePremiumFee(service.getPrice(), priority);
            
            // Create appointment
            Appointment appointment = new Appointment();
            appointment.setClient(user);
            appointment.setProfessional(professional);
            appointment.setService(service);
            appointment.setPriority(priority);
            appointment.setPremiumFee(premiumFee);
            appointment.setUserNotes(request.getUserNotes());
            appointment.setStatus(AppointmentStatus.IN_QUEUE);
            appointment.setTotalAmount(service.getPrice());
            
            if (request.getAppointmentDateTime() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                OffsetDateTime offsetDateTime = OffsetDateTime.parse(request.getAppointmentDateTime());
                appointment.setStartTime(offsetDateTime);
                appointment.setEndTime(offsetDateTime.plusMinutes(service.getDurationMinutes()));
            }
            
            Appointment saved = appointmentRepository.save(appointment);
            
            // Insert with priority (respects fairness rules)
            Integer queuePosition = priorityQueueService.insertWithPriority(
                saved.getId(),
                request.getProfessionalId(),
                priority,
                premiumFee
            );
            
            log.info("Priority booking created: appointmentId={}, priority={}, fee={}, position={}",
                saved.getId(), priority, premiumFee, queuePosition);
            
            AppointmentResponse.Summary response = new AppointmentResponse.Summary();
            response.setId(saved.getId());
            response.setStatus(saved.getStatus().toString());
            response.setPriority(priority.name());
            response.setTotalAmount(premiumFee);
            response.setCreatedAt(saved.getCreatedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error booking priority appointment", e);
            return ResponseEntity.status(500).body("Booking failed: " + e.getMessage());
        }
    }

    /**
     * Get priority distribution in queue for a professional.
     * Used by professional to see how many PREMIUMs vs NORMALs are ahead.
     */
    @GetMapping("/queue-check/{professionalId}")
    public ResponseEntity<?> checkQueuePriorities(
            @PathVariable UUID professionalId,
            Authentication auth) {
        
        try {
            if (auth == null || auth.getPrincipal() == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            
            PriorityQueueCheckResponse response = priorityQueueService
                    .getPriorityDistribution(professionalId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking queue priorities", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Admin endpoint to override fairness rules for a specific appointment.
     */
    @PostMapping("/override")
    public ResponseEntity<?> adminOverridePriority(
            @RequestParam UUID appointmentId,
            @RequestParam String overridePriority,
            Authentication auth) {
        
        try {
            if (auth == null || !auth.getAuthorities().toString().contains("ADMIN")) {
                return ResponseEntity.status(403).body("Admin access required");
            }
            
            Appointment apt = appointmentRepository.findById(appointmentId)
                    .orElse(null);
            
            if (apt == null) {
                return ResponseEntity.notFound().build();
            }
            
            AppointmentPriority newPriority = AppointmentPriority.valueOf(overridePriority);
            apt.setPriority(newPriority);
            
            Appointment updated = appointmentRepository.save(apt);
            
            log.info("Admin override: appointmentId={}, newPriority={}", appointmentId, newPriority);
            
            AppointmentResponse.Summary response = new AppointmentResponse.Summary();
            response.setId(updated.getId());
            response.setStatus(updated.getStatus().toString());
            response.setPriority(updated.getPriority().name());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error overriding priority", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get active SLA breach alerts.
     * Returns different alerts based on user role.
     */
    @GetMapping("/sla-alerts")
    public ResponseEntity<?> getSLAAlerts(Authentication auth) {
        try {
            if (auth == null || auth.getPrincipal() == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            
            UUID userId = UUID.fromString((String) auth.getPrincipal());
            String role = auth.getAuthorities().iterator().next().getAuthority();
            
            List<SLAAlertResponse> alerts = priorityQueueService
                    .getActiveAlerts(userId, role);
            
            return ResponseEntity.ok(alerts);
            
        } catch (Exception e) {
            log.error("Error fetching SLA alerts", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Calculate premium fee based on service price and priority tier.
     */
    private BigDecimal calculatePremiumFee(
            BigDecimal servicePrice,
            AppointmentPriority priority) {
        
        if (priority == AppointmentPriority.NORMAL) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal multiplier = priority == AppointmentPriority.EMERGENCY ?
                new BigDecimal("0.00") :  // Emergency may not charge, varies by config
                new BigDecimal("0.30");    // PREMIUM = 30% of service price
        
        return servicePrice.multiply(multiplier)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Trigger manual SLA check (can be called from a scheduled task or admin action).
     */
    @PostMapping("/check-sla-breaches")
    public ResponseEntity<?> checkSLABreaches(Authentication auth) {
        try {
            if (auth == null || !auth.getAuthorities().toString().contains("ADMIN")) {
                return ResponseEntity.status(403).body("Admin access required");
            }
            
            priorityQueueService.checkAndLogSLABreaches();
            
            return ResponseEntity.ok("SLA check completed");
            
        } catch (Exception e) {
            log.error("Error checking SLA breaches", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
