package com.appointunified;

import com.appointunified.entity.*;
import com.appointunified.enums.*;
import com.appointunified.exception.AppException;
import com.appointunified.repository.*;
import com.appointunified.service.QueueEngineService;
import com.appointunified.service.NotificationServiceV3Extension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueEngineServiceTest {

    @Mock QueueTokenRepository tokenRepository;
    @Mock QueueEventRepository eventRepository;
    @Mock DelayLogRepository delayLogRepository;
    @Mock QueuePauseLogRepository pauseLogRepository;
    @Mock QueueAnalyticsSnapshotRepository snapshotRepository;
    @Mock EmergencySlotRegistryRepository emergencyRegistry;
    @Mock QueueBroadcastRepository broadcastRepository;
    @Mock AppointmentRepository appointmentRepository;
    @Mock ProfessionalRepository professionalRepository;
    @Mock UserRepository userRepository;
    @Mock StringRedisTemplate redis;
    @Mock SimpMessagingTemplate ws;
    @Mock ObjectMapper objectMapper;
    @Mock NotificationServiceV3Extension notificationServiceV3Extension;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks
    QueueEngineService queueEngine;

    private Professional professional;
    private User profUser;
    private Appointment appointment;
    private UUID profId;
    private UUID profUserId;
    private UUID apptId;

    @BeforeEach
    void setUp() {
        profId = UUID.randomUUID();
        profUserId = UUID.randomUUID();
        apptId = UUID.randomUUID();

        profUser = User.builder().id(profUserId).fullName("Dr. Test")
            .phone("+911234567890").role(UserRole.PROFESSIONAL).build();

        professional = new Professional();
        professional.setId(profId);
        professional.setUser(profUser);
        professional.setDisplayName("Dr. Test");
        professional.setSector(Sector.HEALTHCARE);
        professional.setVerificationStatus(VerificationStatus.APPROVED);
        professional.setAcceptingBookings(true);
        professional.setTotalCompleted(0);

        com.appointunified.entity.Service service = new com.appointunified.entity.Service();
        service.setId(UUID.randomUUID());
        service.setName("Consultation");
        service.setDurationMinutes(30);
        service.setPrice(BigDecimal.valueOf(500));
        service.setSector(Sector.HEALTHCARE);
        service.setProfessional(professional);

        User clientUser = User.builder().id(UUID.randomUUID())
            .fullName("Test Patient").phone("+919876543210").role(UserRole.PUBLIC).build();

        appointment = new Appointment();
        appointment.setId(apptId);
        appointment.setClient(clientUser);
        appointment.setProfessional(professional);
        appointment.setService(service);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setPriority(AppointmentPriority.NORMAL);
        appointment.setStartTime(OffsetDateTime.now().plusHours(1));
        appointment.setEndTime(OffsetDateTime.now().plusHours(2));
        appointment.setShareToken("token123");
        appointment.setShareExpiresAt(OffsetDateTime.now().plusDays(30));
        appointment.setCreatedAt(OffsetDateTime.now());
        appointment.setUpdatedAt(OffsetDateTime.now());

        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        when(redis.hasKey(anyString())).thenReturn(false);
    }

    @Test
    void joinQueue_newAppointment_createsToken() {
        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appointment));
        when(tokenRepository.findByAppointmentId(apptId)).thenReturn(Optional.empty());
        when(tokenRepository.findMaxTokenNumber(profId)).thenReturn(0);
        when(tokenRepository.countWaiting(profId)).thenReturn(0L);
        when(tokenRepository.save(any())).thenAnswer(inv -> {
            QueueToken t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(appointmentRepository.save(any())).thenReturn(appointment);
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepository.findByProfessionalIdAndStatusOrderByPosition(any(), any()))
            .thenReturn(Collections.emptyList());

        var result = queueEngine.joinQueue(apptId);

        assertThat(result).isNotNull();
        assertThat(result.tokenNumber()).isEqualTo(1);
        assertThat(result.position()).isEqualTo(1);
        assertThat(result.status()).isEqualTo("WAITING");
        verify(tokenRepository).save(any(QueueToken.class));
    }

    @Test
    void joinQueue_alreadyInQueue_returnsExistingToken() {
        QueueToken existing = QueueToken.builder()
            .id(UUID.randomUUID()).appointment(appointment).professional(professional)
            .tokenNumber(3).position(3).estimatedWaitMins(90).status("WAITING")
            .createdAt(OffsetDateTime.now()).build();

        when(appointmentRepository.findById(apptId)).thenReturn(Optional.of(appointment));
        when(tokenRepository.findByAppointmentId(apptId)).thenReturn(Optional.of(existing));

        var result = queueEngine.joinQueue(apptId);
        assertThat(result.tokenNumber()).isEqualTo(3);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void callNext_noWaiting_throwsBadRequest() {
        when(professionalRepository.findByUserId(profUserId)).thenReturn(Optional.of(professional));
        when(tokenRepository.findByProfessionalIdAndStatusOrderByPosition(profId, "WAITING"))
            .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> queueEngine.callNext(profUserId))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("No clients waiting");
    }

    @Test
    void triggerDelay_invalidRange_throwsBadRequest() {
        when(professionalRepository.findByUserId(profUserId)).thenReturn(Optional.of(professional));

        assertThatThrownBy(() -> queueEngine.triggerDelay(profUserId, 0, "zero"))
            .isInstanceOf(AppException.class);

        assertThatThrownBy(() -> queueEngine.triggerDelay(profUserId, 300, "too much"))
            .isInstanceOf(AppException.class);
    }

    @Test
    void triggerDelay_valid_updatesAllWaitingTokens() {
        QueueToken t1 = buildToken(1, 1, 30);
        QueueToken t2 = buildToken(2, 2, 60);

        when(professionalRepository.findByUserId(profUserId)).thenReturn(Optional.of(professional));
        when(tokenRepository.findByProfessionalIdAndStatusOrderByPosition(profId, "WAITING"))
            .thenReturn(List.of(t1, t2));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(delayLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(valueOps.increment(anyString(), anyLong())).thenReturn(15L);
        when(tokenRepository.findByProfessionalIdAndStatusOrderByPosition(any(), any()))
            .thenReturn(List.of(t1, t2));
        when(tokenRepository.countWaiting(any())).thenReturn(2L);

        var result = queueEngine.triggerDelay(profUserId, 15, "Running late");

        assertThat(result.delayMinutes()).isEqualTo(15);
        assertThat(result.tokensAffected()).isEqualTo(2);
        assertThat(t1.getEstimatedWaitMins()).isEqualTo(45); // 30 + 15
        assertThat(t2.getEstimatedWaitMins()).isEqualTo(75); // 60 + 15
    }

    @Test
    void pauseQueue_alreadyPaused_throwsBadRequest() {
        when(professionalRepository.findByUserId(profUserId)).thenReturn(Optional.of(professional));
        when(redis.hasKey(contains(":paused"))).thenReturn(true);

        assertThatThrownBy(() -> queueEngine.pauseQueue(profUserId, "lunch"))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("already paused");
    }

    @Test
    void getStatus_returnsCorrectView() {
        QueueToken t1 = buildToken(1, 1, 30);
        QueueToken t2 = buildToken(2, 2, 60);

        when(tokenRepository.findByProfessionalIdAndStatusOrderByPosition(profId, "WAITING"))
            .thenReturn(List.of(t1, t2));
        when(tokenRepository.findByProfessionalIdAndStatusOrderByPosition(profId, "CALLED"))
            .thenReturn(Collections.emptyList());
        when(redis.hasKey(any())).thenReturn(false);
        when(tokenRepository.countWaiting(profId)).thenReturn(2L);

        var status = queueEngine.getStatus(profId);

        assertThat(status.waitingCount()).isEqualTo(2);
        assertThat(status.paused()).isFalse();
        assertThat(status.currentlyServing()).isNull();
        assertThat(status.waitingTokens()).hasSize(2);
    }

    private QueueToken buildToken(int number, int position, int waitMins) {
        return QueueToken.builder()
            .id(UUID.randomUUID()).appointment(appointment).professional(professional)
            .tokenNumber(number).position(position).estimatedWaitMins(waitMins)
            .status("WAITING").createdAt(OffsetDateTime.now()).build();
    }
}
