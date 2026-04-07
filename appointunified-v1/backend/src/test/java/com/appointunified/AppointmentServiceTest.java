package com.appointunified;

import com.appointunified.entity.*;
import com.appointunified.enums.*;
import com.appointunified.exception.AppException;
import com.appointunified.repository.*;
import com.appointunified.service.AppointmentService;
import com.appointunified.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock ProfessionalRepository professionalRepository;
    @Mock ServiceRepository serviceRepository;
    @Mock UserRepository userRepository;
    @Mock BookingDraftRepository bookingDraftRepository;
    @Mock NotificationService notificationService;

    @InjectMocks AppointmentService appointmentService;

    private User client;
    private Professional professional;
    private com.appointunified.entity.Service service;
    private UUID clientId;
    private UUID professionalId;
    private UUID serviceId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(appointmentService, "frontendUrl", "http://localhost:3000");

        clientId = UUID.randomUUID();
        professionalId = UUID.randomUUID();
        serviceId = UUID.randomUUID();

        User profUser = new User();
        profUser.setId(UUID.randomUUID());

        client = new User();
        client.setId(clientId);
        client.setFullName("Test Patient");
        client.setPhone("+919876543210");
        client.setRole(UserRole.PUBLIC);

        professional = new Professional();
        professional.setId(professionalId);
        professional.setUser(profUser);
        professional.setDisplayName("Dr. Test");
        professional.setSector(Sector.HEALTHCARE);
        professional.setVerificationStatus(VerificationStatus.APPROVED);
        professional.setAcceptingBookings(true);
        professional.setTotalCompleted(0);

        service = new com.appointunified.entity.Service();
        service.setId(serviceId);
        service.setProfessional(professional);
        service.setSector(Sector.HEALTHCARE);
        service.setName("Consultation");
        service.setDurationMinutes(30);
        service.setPrice(BigDecimal.valueOf(500));
        service.setActive(true);
    }

    @Test
    void getByShareToken_validToken_returnsAppointment() {
        Appointment appt = buildAppointment();
        appt.setShareToken("abc123");
        appt.setShareExpiresAt(OffsetDateTime.now().plusDays(10));

        when(appointmentRepository.findByShareToken("abc123")).thenReturn(Optional.of(appt));

        var result = appointmentService.getByShareToken("abc123");

        assertThat(result).isNotNull();
        assertThat(result.getShareToken()).isEqualTo("abc123");
    }

    @Test
    void getByShareToken_expiredToken_throwsBadRequest() {
        Appointment appt = buildAppointment();
        appt.setShareToken("expired");
        appt.setShareExpiresAt(OffsetDateTime.now().minusDays(1)); // expired

        when(appointmentRepository.findByShareToken("expired")).thenReturn(Optional.of(appt));

        assertThatThrownBy(() -> appointmentService.getByShareToken("expired"))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("expired");
    }

    @Test
    void getByShareToken_unknownToken_throwsNotFound() {
        when(appointmentRepository.findByShareToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.getByShareToken("unknown"))
            .isInstanceOf(AppException.class);
    }

    @Test
    void generateIcal_validAppointment_returnsIcalString() {
        Appointment appt = buildAppointment();

        when(appointmentRepository.findById(appt.getId())).thenReturn(Optional.of(appt));

        String ical = appointmentService.generateIcal(appt.getId());

        assertThat(ical).contains("BEGIN:VCALENDAR");
        assertThat(ical).contains("BEGIN:VEVENT");
        assertThat(ical).contains("END:VEVENT");
        assertThat(ical).contains("END:VCALENDAR");
        assertThat(ical).contains(appt.getId().toString());
        assertThat(ical).contains("Consultation");
        assertThat(ical).contains("Dr. Test");
    }

    @Test
    void cancel_notOwner_throwsForbidden() {
        UUID otherUserId = UUID.randomUUID();
        Appointment appt = buildAppointment();

        when(appointmentRepository.findById(appt.getId())).thenReturn(Optional.of(appt));

        var cancelRequest = new com.appointunified.dto.request.AppointmentRequest.Cancel();

        assertThatThrownBy(() -> appointmentService.cancel(appt.getId(), otherUserId, cancelRequest))
            .isInstanceOf(AppException.class);
    }

    @Test
    void cancel_alreadyCancelled_throwsBadRequest() {
        Appointment appt = buildAppointment();
        appt.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(appt.getId())).thenReturn(Optional.of(appt));

        var cancelRequest = new com.appointunified.dto.request.AppointmentRequest.Cancel();

        assertThatThrownBy(() -> appointmentService.cancel(appt.getId(), clientId, cancelRequest))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("cancel");
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Appointment buildAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setClient(client);
        appointment.setProfessional(professional);
        appointment.setService(service);
        appointment.setStartTime(OffsetDateTime.now().plusDays(1));
        appointment.setEndTime(OffsetDateTime.now().plusDays(1).plusMinutes(30));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setPriority(AppointmentPriority.NORMAL);
        appointment.setVirtual(false);
        appointment.setShareToken("testtoken");
        appointment.setShareExpiresAt(OffsetDateTime.now().plusDays(30));
        appointment.setCreatedAt(OffsetDateTime.now());
        appointment.setUpdatedAt(OffsetDateTime.now());
        return appointment;
    }
}
