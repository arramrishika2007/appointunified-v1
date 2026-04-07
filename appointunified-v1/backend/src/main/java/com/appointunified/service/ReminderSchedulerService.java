package com.appointunified.service;

import com.appointunified.entity.Appointment;
import com.appointunified.enums.AppointmentStatus;
import com.appointunified.repository.AppointmentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NEW V1 FEATURE 5: Appointment Reminder Scheduler
 *
 * Sends proactive reminder notifications at configurable intervals
 * before each appointment. Unlike basic calendar reminders, this
 * system is context-aware: it respects user notification preferences
 * and skips if the appointment has already been cancelled/completed.
 *
 * Reminder windows: 24h, 2h, 30min before appointment
 */
@Service
public class ReminderSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(ReminderSchedulerService.class);

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    public ReminderSchedulerService(AppointmentRepository appointmentRepository,
                                    NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.notificationService = notificationService;
    }

    /**
     * 24-hour reminder — runs every hour, finds appointments starting ~24h from now
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour on the hour
    @Transactional(readOnly = true)
    public void send24HourReminders() {
        OffsetDateTime windowStart = OffsetDateTime.now().plusHours(23).plusMinutes(50);
        OffsetDateTime windowEnd   = OffsetDateTime.now().plusHours(24).plusMinutes(10);

        List<Appointment> appointments = findScheduledInWindow(windowStart, windowEnd);
        log.info("24h reminder window: found {} appointments", appointments.size());

        appointments.forEach(appt -> {
            try {
                notificationService.sendAppointmentReminder(appt.getId(), 24);
            } catch (Exception e) {
                log.error("Failed 24h reminder for appt {}: {}", appt.getId(), e.getMessage());
            }
        });
    }

    /**
     * 2-hour reminder — runs every 15 minutes
     */
    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    @Transactional(readOnly = true)
    public void send2HourReminders() {
        OffsetDateTime windowStart = OffsetDateTime.now().plusHours(1).plusMinutes(55);
        OffsetDateTime windowEnd   = OffsetDateTime.now().plusHours(2).plusMinutes(5);

        List<Appointment> appointments = findScheduledInWindow(windowStart, windowEnd);
        log.info("2h reminder window: found {} appointments", appointments.size());

        appointments.forEach(appt -> {
            try {
                notificationService.sendAppointmentReminder(appt.getId(), 2);
            } catch (Exception e) {
                log.error("Failed 2h reminder for appt {}: {}", appt.getId(), e.getMessage());
            }
        });
    }

    /**
     * 30-minute reminder — runs every 5 minutes
     */
    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    @Transactional(readOnly = true)
    public void send30MinReminders() {
        OffsetDateTime windowStart = OffsetDateTime.now().plusMinutes(25);
        OffsetDateTime windowEnd   = OffsetDateTime.now().plusMinutes(35);

        List<Appointment> appointments = findScheduledInWindow(windowStart, windowEnd);
        log.info("30min reminder window: found {} appointments", appointments.size());

        appointments.forEach(appt -> {
            try {
                notificationService.sendAppointmentReminder(appt.getId(), 0); // 0 = "very soon"
            } catch (Exception e) {
                log.error("Failed 30min reminder for appt {}: {}", appt.getId(), e.getMessage());
            }
        });
    }

    /**
     * Auto-expire past appointments that were never marked — runs daily at 1 AM
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void autoExpirePastAppointments() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(2);
        List<Appointment> stale = appointmentRepository.findAll().stream()
            .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED
                      && a.getEndTime().isBefore(cutoff))
            .toList();

        stale.forEach(a -> {
            a.setStatus(AppointmentStatus.EXPIRED);
            appointmentRepository.save(a);
        });

        if (!stale.isEmpty()) {
            log.info("Auto-expired {} stale appointments", stale.size());
        }
    }

    private List<Appointment> findScheduledInWindow(OffsetDateTime from, OffsetDateTime to) {
        return appointmentRepository.findAll().stream()
            .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED
                      && a.getStartTime().isAfter(from)
                      && a.getStartTime().isBefore(to))
            .toList();
    }
}
