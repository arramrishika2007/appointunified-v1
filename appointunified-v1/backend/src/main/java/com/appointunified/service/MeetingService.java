package com.appointunified.service;

import com.appointunified.entity.Appointment;
import com.appointunified.entity.User;
import com.appointunified.enums.AppointmentStatus;
import com.appointunified.exception.AppException;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.UserRepository;
import com.appointunified.security.JwtTokenProvider;
import com.appointunified.controller.MeetingController.MeetingJoinResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.meeting.strict-window:false}")
    private boolean strictMeetingJoinWindow;

    @Transactional
    public MeetingJoinResponse generateJoinToken(UUID bookingId, UUID userId) {
        Appointment appointment = appointmentRepository.findById(bookingId)
                .orElseThrow(() -> AppException.notFound("Appointment not found"));

        if (!appointment.isVirtual()) {
            throw AppException.badRequest("This appointment is not a virtual meeting");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        boolean isClient = appointment.getClient().getId().equals(userId);
        boolean isProfessional = appointment.getProfessional().getUser().getId().equals(userId);

        if (!isClient && !isProfessional) {
            throw AppException.forbidden("You do not have access to this meeting");
        }

        AppointmentStatus status = appointment.getStatus();
           // Allow joining for all active meeting lifecycle states.
        if (status != AppointmentStatus.CONFIRMED && 
            status != AppointmentStatus.DEPOSIT_PAID && 
            status != AppointmentStatus.PAID_FULL && 
            status != AppointmentStatus.SCHEDULED && 
              status != AppointmentStatus.IN_MEETING &&
              status != AppointmentStatus.IN_PROGRESS &&
              status != AppointmentStatus.PENDING_BALANCE &&
              status != AppointmentStatus.COMPLETED) {
            throw AppException.badRequest("Meeting is not available in current state: " + status.name());
        }

           // Time window check: 15 mins before to 30 mins after (optional in demo mode).
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime joinStartsAt = appointment.getStartTime().minusMinutes(15);
        OffsetDateTime joinEndsAt = appointment.getEndTime().plusMinutes(30);

           if (strictMeetingJoinWindow) {
              if (now.isBefore(joinStartsAt)) {
                  throw AppException.badRequest("Meeting has not started yet");
              }
              if (now.isAfter(joinEndsAt)) {
                  throw AppException.badRequest("Meeting join window has ended");
              }
        }

        // If Professional is joining, officially transition state to IN_MEETING
        if (isProfessional && (status == AppointmentStatus.SCHEDULED || status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.DEPOSIT_PAID)) {
             appointment.setStatus(AppointmentStatus.IN_MEETING);
             appointmentRepository.save(appointment);
        }

        String roomName = "appointunified-" + appointment.getId().toString().replace("-", "");
        boolean isModerator = isProfessional;

        // Generate Jitsi specific token
        String jwtToken = jwtTokenProvider.generateJitsiMeetingToken(
                userId,
                user.getFullName(),
                user.getAvatarUrl(),
                roomName,
                isModerator
        );

        log.info("Generated Jitsi token (moderator={}) for Appointment {} and User {}", isModerator, bookingId, userId);
        return new MeetingJoinResponse(roomName, jwtToken, appointment.getStatus().name());
    }
}
